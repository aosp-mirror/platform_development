/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* this implements a sensors hardware library for the Android emulator.
 * the following code should be built as a shared library that will be
 * placed into /system/lib/hw/sensors.goldfish.so
 *
 * it will be loaded by the code in hardware/libhardware/hardware.c
 * which is itself called from com_android_server_SensorService.cpp
 */


/* we connect with the emulator through the "sensors" qemud service
 */
#define  SENSORS_SERVICE_NAME "sensors"

#define LOG_TAG "QemuSensors"

#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <string.h>
#include <cutils/log.h>
#include <cutils/native_handle.h>
#include <cutils/sockets.h>
#include <hardware/sensors.h>

#if 0
#define  D(...)  LOGD(__VA_ARGS__)
#else
#define  D(...)  ((void)0)
#endif

#define  E(...)  LOGE(__VA_ARGS__)

#include <hardware/qemud.h>

/** SENSOR IDS AND NAMES
 **/

#define MAX_NUM_SENSORS 4

#define SUPPORTED_SENSORS  ((1<<MAX_NUM_SENSORS)-1)

#define  ID_BASE           SENSORS_HANDLE_BASE
#define  ID_ACCELERATION   (ID_BASE+0)
#define  ID_MAGNETIC_FIELD (ID_BASE+1)
#define  ID_ORIENTATION    (ID_BASE+2)
#define  ID_TEMPERATURE    (ID_BASE+3)

#define  SENSORS_ACCELERATION   (1 << ID_ACCELERATION)
#define  SENSORS_MAGNETIC_FIELD  (1 << ID_MAGNETIC_FIELD)
#define  SENSORS_ORIENTATION     (1 << ID_ORIENTATION)
#define  SENSORS_TEMPERATURE     (1 << ID_TEMPERATURE)

#define  ID_CHECK(x)  ((unsigned)((x)-ID_BASE) < 4)

#define  SENSORS_LIST  \
    SENSOR_(ACCELERATION,"acceleration") \
    SENSOR_(MAGNETIC_FIELD,"magnetic-field") \
    SENSOR_(ORIENTATION,"orientation") \
    SENSOR_(TEMPERATURE,"temperature") \

static const struct {
    const char*  name;
    int          id; } _sensorIds[MAX_NUM_SENSORS] =
{
#define SENSOR_(x,y)  { y, ID_##x },
    SENSORS_LIST
#undef  SENSOR_
};

static const char*
_sensorIdToName( int  id )
{
    int  nn;
    for (nn = 0; nn < MAX_NUM_SENSORS; nn++)
        if (id == _sensorIds[nn].id)
            return _sensorIds[nn].name;
    return "<UNKNOWN>";
}

static int
_sensorIdFromName( const char*  name )
{
    int  nn;

    if (name == NULL)
        return -1;

    for (nn = 0; nn < MAX_NUM_SENSORS; nn++)
        if (!strcmp(name, _sensorIds[nn].name))
            return _sensorIds[nn].id;

    return -1;
}

/** SENSORS CONTROL DEVICE
 **
 ** This one is used to send commands to the sensors drivers.
 ** We implement this by sending directly commands to the emulator
 ** through the QEMUD channel.
 **/

typedef struct SensorControl {
    struct sensors_control_device_t  device;
    int                              fd;
    uint32_t                         active_sensors;
} SensorControl;

/* this must return a file descriptor that will be used to read
 * the sensors data (it is passed to data__data_open() below
 */
static native_handle_t*
control__open_data_source(struct sensors_control_device_t *dev)
{
    SensorControl*  ctl = (void*)dev;
    native_handle_t* handle;

    if (ctl->fd < 0) {
        ctl->fd = qemud_channel_open(SENSORS_SERVICE_NAME);
    }
    D("%s: fd=%d", __FUNCTION__, ctl->fd);
    handle = native_handle_create(1, 0);
    handle->data[0] = ctl->fd;
    return handle;
}

static int
control__activate(struct sensors_control_device_t *dev,
                  int handle,
                  int enabled)
{
    SensorControl*  ctl = (void*)dev;
    uint32_t        mask, sensors, active, new_sensors, changed;
    char            command[128];
    int             ret;

    D("%s: handle=%s (%d) enabled=%d", __FUNCTION__,
        _sensorIdToName(handle), handle, enabled);

    if (!ID_CHECK(handle)) {
        E("%s: bad handle ID", __FUNCTION__);
        return -1;
    }

    mask    = (1<<handle);
    sensors = enabled ? mask : 0;

    active      = ctl->active_sensors;
    new_sensors = (active & ~mask) | (sensors & mask);
    changed     = active ^ new_sensors;

    if (!changed)
        return 0;

    snprintf(command, sizeof command, "set:%s:%d",
                _sensorIdToName(handle), enabled != 0);

    if (ctl->fd < 0) {
        ctl->fd = qemud_channel_open(SENSORS_SERVICE_NAME);
    }

    ret = qemud_channel_send(ctl->fd, command, -1);
    if (ret < 0)
        return -1;

    ctl->active_sensors = new_sensors;

    return 0;
}

static int
control__set_delay(struct sensors_control_device_t *dev, int32_t ms)
{
    SensorControl*  ctl = (void*)dev;
    char            command[128];

    D("%s: dev=%p delay-ms=%d", __FUNCTION__, dev, ms);

    snprintf(command, sizeof command, "set-delay:%d", ms);

    return qemud_channel_send(ctl->fd, command, -1);
}

/* this function is used to force-stop the blocking read() in
 * data__poll. In order to keep the implementation as simple
 * as possible here, we send a command to the emulator which
 * shall send back an appropriate data block to the system.
 */
static int
control__wake(struct sensors_control_device_t *dev)
{
    SensorControl*  ctl = (void*)dev;
    D("%s: dev=%p", __FUNCTION__, dev);
    return qemud_channel_send(ctl->fd, "wake", -1);
}


static int
control__close(struct hw_device_t *dev) 
{
    SensorControl*  ctl = (void*)dev;
    close(ctl->fd);
    free(ctl);
    return 0;
}

/** SENSORS DATA DEVICE
 **
 ** This one is used to read sensor data from the hardware.
 ** We implement this by simply reading the data from the
 ** emulator through the QEMUD channel.
 **/


typedef struct SensorData {
    struct sensors_data_device_t  device;
    sensors_data_t                sensors[MAX_NUM_SENSORS];
    int                           events_fd;
    uint32_t                      pendingSensors;
    int64_t                       timeStart;
    int64_t                       timeOffset;
} SensorData;

/* return the current time in nanoseconds */
static int64_t
data__now_ns(void)
{
    struct timespec  ts;

    clock_gettime(CLOCK_MONOTONIC, &ts);

    return (int64_t)ts.tv_sec * 1000000000 + ts.tv_nsec;
}

static int
data__data_open(struct sensors_data_device_t *dev, native_handle_t* handle)
{
    SensorData*  data = (void*)dev;
    int i;
    D("%s: dev=%p fd=%d", __FUNCTION__, dev, fd);
    memset(&data->sensors, 0, sizeof(data->sensors));

    for (i=0 ; i<MAX_NUM_SENSORS ; i++) {
        data->sensors[i].vector.status = SENSOR_STATUS_ACCURACY_HIGH;
    }
    data->pendingSensors = 0;
    data->timeStart      = 0;
    data->timeOffset     = 0;

    data->events_fd = dup(handle->data[0]);
    native_handle_close(handle);
    native_handle_delete(handle);
    return 0;
}

static int
data__data_close(struct sensors_data_device_t *dev)
{
    SensorData*  data = (void*)dev;
    D("%s: dev=%p", __FUNCTION__, dev);
    if (data->events_fd > 0) {
        close(data->events_fd);
        data->events_fd = -1;
    }
    return 0;
}

static int
pick_sensor(SensorData*      data,
            sensors_data_t*  values)
{
    uint32_t mask = SUPPORTED_SENSORS;
    while (mask) {
        uint32_t i = 31 - __builtin_clz(mask);
        mask &= ~(1<<i);
        if (data->pendingSensors & (1<<i)) {
            data->pendingSensors &= ~(1<<i);
            *values = data->sensors[i];
            values->sensor = (1<<i);
            LOGD_IF(0, "%s: %d [%f, %f, %f]", __FUNCTION__,
                    (1<<i),
                    values->vector.x,
                    values->vector.y,
                    values->vector.z);
            return i;
        }
    }
    LOGE("No sensor to return!!! pendingSensors=%08x", data->pendingSensors);
    // we may end-up in a busy loop, slow things down, just in case.
    usleep(100000);
    return -1;
}

static int
data__poll(struct sensors_data_device_t *dev, sensors_data_t* values)
{
    SensorData*  data = (void*)dev;
    int fd = data->events_fd;

    D("%s: data=%p", __FUNCTION__, dev);

    // there are pending sensors, returns them now...
    if (data->pendingSensors) {
        return pick_sensor(data, values);
    }

    // wait until we get a complete event for an enabled sensor
    uint32_t new_sensors = 0;

    while (1) {
        /* read the next event */
        char     buff[256];
        int      len = qemud_channel_recv(data->events_fd, buff, sizeof buff-1);
        float    params[3];
        int64_t  event_time;

        if (len < 0)
            continue;

        buff[len] = 0;

        /* "wake" is sent from the emulator to exit this loop. This shall
         * really be because another thread called "control__wake" in this
         * process.
         */
        if (!strcmp((const char*)data, "wake")) {
            return 0x7FFFFFFF;
        }

        /* "acceleration:<x>:<y>:<z>" corresponds to an acceleration event */
        if (sscanf(buff, "acceleration:%g:%g:%g", params+0, params+1, params+2) == 3) {
            new_sensors |= SENSORS_ACCELERATION;
            data->sensors[ID_ACCELERATION].acceleration.x = params[0];
            data->sensors[ID_ACCELERATION].acceleration.y = params[1];
            data->sensors[ID_ACCELERATION].acceleration.z = params[2];
            continue;
        }

        /* "orientation:<azimuth>:<pitch>:<roll>" is sent when orientation changes */
        if (sscanf(buff, "orientation:%g:%g:%g", params+0, params+1, params+2) == 3) {
            new_sensors |= SENSORS_ORIENTATION;
            data->sensors[ID_ORIENTATION].orientation.azimuth = params[0];
            data->sensors[ID_ORIENTATION].orientation.pitch   = params[1];
            data->sensors[ID_ORIENTATION].orientation.roll    = params[2];
            continue;
        }

        /* "magnetic:<x>:<y>:<z>" is sent for the params of the magnetic field */
        if (sscanf(buff, "magnetic:%g:%g:%g", params+0, params+1, params+2) == 3) {
            new_sensors |= SENSORS_MAGNETIC_FIELD;
            data->sensors[ID_MAGNETIC_FIELD].magnetic.x = params[0];
            data->sensors[ID_MAGNETIC_FIELD].magnetic.y = params[1];
            data->sensors[ID_MAGNETIC_FIELD].magnetic.z = params[2];
            continue;
        }

        /* "temperature:<celsius>" */
        if (sscanf(buff, "temperature:%g", params+0) == 2) {
            new_sensors |= SENSORS_TEMPERATURE;
            data->sensors[ID_TEMPERATURE].temperature = params[0];
            continue;
        }

        /* "sync:<time>" is sent after a series of sensor events.
         * where 'time' is expressed in micro-seconds and corresponds
         * to the VM time when the real poll occured.
         */
        if (sscanf(buff, "sync:%lld", &event_time) == 1) {
            if (new_sensors) {
                data->pendingSensors = new_sensors;
                int64_t t = event_time * 1000LL;  /* convert to nano-seconds */

                /* use the time at the first sync: as the base for later
                 * time values */
                if (data->timeStart == 0) {
                    data->timeStart  = data__now_ns();
                    data->timeOffset = data->timeStart - t;
                }
                t += data->timeOffset;

                while (new_sensors) {
                    uint32_t i = 31 - __builtin_clz(new_sensors);
                    new_sensors &= ~(1<<i);
                    data->sensors[i].time = t;
                }
                return pick_sensor(data, values);
            } else {
                D("huh ? sync without any sensor data ?");
            }
            continue;
        }
        D("huh ? unsupported command");
    }
}

static int
data__close(struct hw_device_t *dev) 
{
    SensorData* data = (SensorData*)dev;
    if (data) {
        if (data->events_fd > 0) {
            //LOGD("(device close) about to close fd=%d", data->events_fd);
            close(data->events_fd);
        }
        free(data);
    }
    return 0;
}


/** MODULE REGISTRATION SUPPORT
 **
 ** This is required so that hardware/libhardware/hardware.c
 ** will dlopen() this library appropriately.
 **/

/*
 * the following is the list of all supported sensors.
 * this table is used to build sSensorList declared below
 * according to which hardware sensors are reported as
 * available from the emulator (see get_sensors_list below)
 *
 * note: numerical values for maxRange/resolution/power were
 *       taken from the reference AK8976A implementation
 */
static const struct sensor_t sSensorListInit[] = {
        { .name       = "Goldfish 3-axis Accelerometer",
          .vendor     = "The Android Open Source Project",
          .version    = 1,
          .handle     = ID_ACCELERATION,
          .type       = SENSOR_TYPE_ACCELEROMETER,
          .maxRange   = 2.8f,
          .resolution = 1.0f/4032.0f,
          .power      = 3.0f,
          .reserved   = {}
        },

        { .name       = "Goldfish 3-axis Magnetic field sensor",
          .vendor     = "The Android Open Source Project",
          .version    = 1,
          .handle     = ID_MAGNETIC_FIELD,
          .type       = SENSOR_TYPE_MAGNETIC_FIELD,
          .maxRange   = 2000.0f,
          .resolution = 1.0f,
          .power      = 6.7f,
          .reserved   = {}
        },

        { .name       = "Goldfish Orientation sensor",
          .vendor     = "The Android Open Source Project",
          .version    = 1,
          .handle     = ID_ORIENTATION,
          .type       = SENSOR_TYPE_ORIENTATION,
          .maxRange   = 360.0f,
          .resolution = 1.0f,
          .power      = 9.7f,
          .reserved   = {}
        },

        { .name       = "Goldfish Temperature sensor",
          .vendor     = "The Android Open Source Project",
          .version    = 1,
          .handle     = ID_TEMPERATURE,
          .type       = SENSOR_TYPE_TEMPERATURE,
          .maxRange   = 80.0f,
          .resolution = 1.0f,
          .power      = 0.0f,
          .reserved   = {}
        },
};

static struct sensor_t  sSensorList[MAX_NUM_SENSORS];

static uint32_t sensors__get_sensors_list(struct sensors_module_t* module,
        struct sensor_t const** list) 
{
    int  fd = qemud_channel_open(SENSORS_SERVICE_NAME);
    char buffer[12];
    int  mask, nn, count;

    int  ret;
    if (fd < 0) {
        E("%s: no qemud connection", __FUNCTION__);
        return 0;
    }
    ret = qemud_channel_send(fd, "list-sensors", -1);
    if (ret < 0) {
        E("%s: could not query sensor list: %s", __FUNCTION__,
          strerror(errno));
        close(fd);
        return 0;
    }
    ret = qemud_channel_recv(fd, buffer, sizeof buffer-1);
    if (ret < 0) {
        E("%s: could not receive sensor list: %s", __FUNCTION__,
          strerror(errno));
        close(fd);
        return 0;
    }
    buffer[ret] = 0;
    close(fd);

    /* the result is a integer used as a mask for available sensors */
    mask  = atoi(buffer);
    count = 0;
    for (nn = 0; nn < MAX_NUM_SENSORS; nn++) {
        if (((1 << nn) & mask) == 0)
            continue;

        sSensorList[count++] = sSensorListInit[nn];
    }
    D("%s: returned %d sensors (mask=%d)", __FUNCTION__, count, mask);
    *list = sSensorList;
    return count;
}


static int
open_sensors(const struct hw_module_t* module,
             const char*               name,
             struct hw_device_t*      *device)
{
    int  status = -EINVAL;

    D("%s: name=%s", __FUNCTION__, name);

    if (!strcmp(name, SENSORS_HARDWARE_CONTROL))
    {
        SensorControl *dev = malloc(sizeof(*dev));

        memset(dev, 0, sizeof(*dev));

        dev->device.common.tag       = HARDWARE_DEVICE_TAG;
        dev->device.common.version   = 0;
        dev->device.common.module    = (struct hw_module_t*) module;
        dev->device.common.close     = control__close;
        dev->device.open_data_source = control__open_data_source;
        dev->device.activate         = control__activate;
        dev->device.set_delay        = control__set_delay;
        dev->device.wake             = control__wake;
        dev->fd                      = -1;

        *device = &dev->device.common;
        status  = 0;
    }
    else if (!strcmp(name, SENSORS_HARDWARE_DATA)) {
        SensorData *dev = malloc(sizeof(*dev));

        memset(dev, 0, sizeof(*dev));

        dev->device.common.tag     = HARDWARE_DEVICE_TAG;
        dev->device.common.version = 0;
        dev->device.common.module  = (struct hw_module_t*) module;
        dev->device.common.close   = data__close;
        dev->device.data_open      = data__data_open;
        dev->device.data_close     = data__data_close;
        dev->device.poll           = data__poll;
        dev->events_fd             = -1;

        *device = &dev->device.common;
        status  = 0;
    }
    return status;
}


static struct hw_module_methods_t sensors_module_methods = {
    .open = open_sensors
};

const struct sensors_module_t HAL_MODULE_INFO_SYM = {
    .common = {
        .tag = HARDWARE_MODULE_TAG,
        .version_major = 1,
        .version_minor = 0,
        .id = SENSORS_HARDWARE_MODULE_ID,
        .name = "Goldfish SENSORS Module",
        .author = "The Android Open Source Project",
        .methods = &sensors_module_methods,
    },
    .get_sensors_list = sensors__get_sensors_list
};
