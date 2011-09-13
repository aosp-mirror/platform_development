/*
 * Copyright (C) 2011 The Android Open Source Project
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

#ifndef HW_EMULATOR_CAMERA_QEMU_CLIENT_H
#define HW_EMULATOR_CAMERA_QEMU_CLIENT_H

/*
 * Contains declaration of classes that encapsulate connection to camera services
 * in the emulator via qemu pipe.
 */

#include <hardware/qemud.h>

namespace android {

/****************************************************************************
 * Qemu query
 ***************************************************************************/

/* Encapsulates a query to the emulator.
 * Guest exchanges data with the emulator via queries sent over the qemu pipe.
 * The queries as well as replies to the queries are all strings (except for the
 * 'frame' query where reply is a framebuffer).
 * Each query is formatted as such:
 *
 *      "<query name>[ <parameters>]",
 *
 * where <query name> is a string representing query name, and <parameters> are
 * optional parameters for the query. If parameters are present, they must be
 * separated from the query name with a single space, and they must be formatted
 * as such:
 *
 *      "<name1>=<value1> <name2>=<value2> ... <nameN>=<valueN>"
 *
 * I.e.:
 *  - Every parameter must have a name, and a value.
 *  - Name and value must be separated with '='.
 *  - No spaces are allowed around '=' separating name and value.
 *  - Parameters must be separated with a single space character.
 *  - No '=' character is allowed in name and in value.
 *
 * There are certain restrictions on strings used in the query:
 *  - Spaces are allowed only as separators.
 *  - '=' are allowed only to divide parameter names from parameter values.
 *
 * Emulator replies to each query in two chunks:
 * - 4 bytes encoding the payload size
 * - Payload, whose size is defined by the first chunk.
 *
 * Every payload always begins with two characters, encoding the result of the
 * query:
 *  - 'ok' Encoding the success
 *  - 'ko' Encoding a failure.
 * After that payload may have optional data. If payload has more data following
 * the query result, there is a ':' character separating them. If payload carries
 * only the result, it always ends with a zero-terminator.
 */
class QemuQuery {
public:
    /* Constructs an uninitialized QemuQuery instance. */
    QemuQuery();

    /* Constructs and initializes QemuQuery instance for a query.
     * Param:
     *  query_string - Query string. This constructor can also be used to
     *      construct a query that doesn't have parameters. In this case query
     *      name can be passed as a parameter here.
     */
    explicit QemuQuery(const char* query_string);

    /* Constructs and initializes QemuQuery instance for a query.
     * Param:
     *  query_name - Query name.
     *  query_param - Query parameters. Can be NULL.
     */
    QemuQuery(const char* query_name, const char* query_param);

    /* Destructs QemuQuery instance. */
    ~QemuQuery();

    /****************************************************************************
     * Public API
     ***************************************************************************/

    /* Creates new query.
     * This method will reset this instance prior to creating a new query.
     * Param:
     *  query_name - Query name.
     *  query_param - Query parameters. Can be NULL.
     * Return:
     *  NO_ERROR on success, or an appropriate error status.
     */
    status_t createQuery(const char* name, const char* param);

    /* Completes the query after a reply from the emulator.
     * This method will parse the reply buffer, and calculate the final query
     * status, which depends not only on the transport success / failure, but
     * also on 'ok' / 'ko' in the query reply.
     * Param:
     *  status - Query delivery status. This status doesn't necessarily reflects
     *  the final query status (which is defined by 'ok'/'ko' in the reply buffer).
     *  This status simply states whether or not the query has been sent, and a
     *  reply has been received successfuly. However, if status indicates a
     *  failure, the entire query has failed. If status indicates a success, the
     *  reply will be checked here to calculate the final query status.
     * Return:
     *  NO_ERROR on success, or an appropriate error status on failure. Note that
     *  status returned here just signals whether or not the method has succeeded.
     *  Use isQuerySucceeded() / getCompletionStatus() methods to check the final
     *  query status.
     */
    status_t completeQuery(status_t status);

    /* Resets the query from a previous use. */
    void resetQuery();

    /* Checks if query has succeeded.
     * Note that this method must be called after completeQuery() method of this
     * class has been executed.
     */
    inline bool isQuerySucceeded() const {
        return mQueryStatus == NO_ERROR && mReplyStatus != 0;
    }

    /* Gets final completion status of the query.
     * Note that this method must be called after completeQuery() method of this
     * class has been executed.
     *  NO_ERROR on success, or an appropriate error status on failure.
     */
    inline status_t getCompletionStatus() const {
        if (isQuerySucceeded()) {
            return NO_ERROR;
        }
        return (mQueryStatus != NO_ERROR) ? mQueryStatus : EINVAL;
    }

    /****************************************************************************
     * Public data memebers
     ***************************************************************************/

public:
    /* Query string. */
    char*       mQuery;
    /* Query status. */
    status_t    mQueryStatus;
    /* Reply buffer */
    char*       mReplyBuffer;
    /* Reply data (past 'ok'/'ko'). If NULL, there were no data in reply. */
    char*       mReplyData;
    /* Reply buffer size. */
    size_t      mReplySize;
    /* Reply data size. */
    size_t      mReplyDataSize;
    /* Reply status: 1 - ok, 0 - ko. */
    int         mReplyStatus;

    /****************************************************************************
     * Private data memebers
     ***************************************************************************/

protected:
    /* Preallocated buffer for small queries. */
    char    mQueryPrealloc[256];
};

/****************************************************************************
 * Qemu client base
 ***************************************************************************/

/* Encapsulates a connection to the 'camera' service in the emulator via qemu
 * pipe.
 */
class QemuClient {
public:
    /* Constructs QemuClient instance. */
    QemuClient();

    /* Destructs QemuClient instance. */
    virtual ~QemuClient();

    /****************************************************************************
     * Qemu client API
     ***************************************************************************/

public:
    /* Connects to the 'camera' service in the emulator via qemu pipe.
     * Param:
     *  param - Parameters to pass to the camera service. There are two types of
     *      camera services implemented by the emulator. The first one is a
     *      'camera factory' type of service that provides list of cameras
     *      connected to the host. Another one is an 'emulated camera' type of
     *      service that provides interface to a camera connected to the host. At
     *      the connection time emulator makes distinction between the two by
     *      looking at connection parameters: no parameters means connection to
     *      the 'factory' service, while connection with parameters means
     *      connection to an 'emulated camera' service, where camera is identified
     *      by one of the connection parameters. So, passing NULL, or an empty
     *      string to this method will establish connection with a 'factory'
     *      service, while not empty string passed here will establish connection
     *      with an 'emulated camera' service. Parameters defining the emulated
     *      camera must be formatted as such:
     *
     *          "name=<device name> [inp_channel=<input channel #>]",
     *
     *      where 'device name' is a required parameter defining name of the
     *      camera device, 'input channel' is an optional parameter (positive
     *      integer), defining input channel to use on the camera device. Note
     *      that device name passed here must have been previously obtained from
     *      the factory service.
     * Return:
     *  NO_ERROR on success, or an appropriate error status.
     */
    virtual status_t connectClient(const char* param);

    /* Disconnects from the service. */
    virtual void disconnectClient();

    /* Sends data to the service.
     * Param:
     *  data, data_size - Data to send.
     * Return:
     *  NO_ERROR on success, or an appropriate error status on failure.
     */
    virtual status_t sendMessage(const void* data, size_t data_size);

    /* Receives data from the service.
     * This method assumes that data to receive will come in two chunks: 8
     * characters encoding the payload size in hexadecimal string, followed by
     * the paylod (if any).
     * This method will allocate data buffer where to receive the response.
     * Param:
     *  data - Upon success contains address of the allocated data buffer with
     *      the data received from the service. The caller is responsible for
     *      freeing allocated data buffer.
     *  data_size - Upon success contains size of the data received from the
     *      service.
     * Return:
     *  NO_ERROR on success, or an appropriate error status on failure.
     */
    virtual status_t receiveMessage(void** data, size_t* data_size);

    /* Sends a query, and receives a response from the service.
     * Param:
     *  query - Query to send to the service. When this method returns, the query
     *  is completed, and all its relevant data members are properly initialized.
     * Return:
     *  NO_ERROR on success, or an appropriate error status on failure. Note that
     *  status returned here is not the final query status. Use isQuerySucceeded(),
     *  or getCompletionStatus() method on the query to see if it has succeeded.
     *  However, if this method returns a failure, it means that the query has
     *  failed, and there is no guarantee that its data members are properly
     *  initialized (except for the 'mQueryStatus', which is always in the
     *  proper state).
     */
    virtual status_t doQuery(QemuQuery* query);

    /****************************************************************************
     * Data members
     ***************************************************************************/

protected:
    /* Qemu pipe handle. */
    int     mPipeFD;

private:
    /* Camera service name. */
    static const char mCameraServiceName[];
};

/****************************************************************************
 * Qemu client for the 'factory' service.
 ***************************************************************************/

/* Encapsulates QemuClient for the 'factory' service. */
class FactoryQemuClient : public QemuClient {
public:
    /* Constructs FactoryQemuClient instance. */
    FactoryQemuClient();

    /* Destructs FactoryQemuClient instance. */
    ~FactoryQemuClient();

    /****************************************************************************
     * Public API
     ***************************************************************************/

public:
    /* Lists camera devices connected to the host.
     * Param:
     *  list - Upon success contains list of cameras connected to the host. The
     *      list returned here is represented as a string, containing multiple
     *      lines, separated with '\n', where each line represents a camera. Each
     *      camera line is formatted as such:
     *
     *          "name=<device name> channel=<num> pix=<num> framedims=<dimensions>\n"
     *
     *      Where:
     *      - 'name' is the name of camera device attached to the host. This name
     *        must be used for subsequent connection to the 'emulated camera'
     *        service for that camera.
     *      - 'channel' - input channel number (positive int) to use to communicate
     *        with the camera.
     *      - 'pix' - pixel format (a "fourcc" int), chosen for the video frames.
     *      - 'framedims' contains a list of frame dimensions supported by the
     *        camera. Each etry in the list is in form '<width>x<height>', where
     *        'width' and 'height' are numeric values for width and height of a
     *        supported frame dimension. Entries in this list are separated with
     *        ','.
     * Return:
     *  NO_ERROR on success, or an appropriate error status on failure.
     */
    status_t listCameras(char** list);

    /****************************************************************************
     * Names of the queries available for the emulated camera factory.
     ***************************************************************************/

private:
    /* List cameras connected to the host. */
    static const char mQueryList[];
};

/****************************************************************************
 * Qemu client for an 'emulated camera' service.
 ***************************************************************************/

/* Encapsulates QemuClient for an 'emulated camera' service.
 */
class CameraQemuClient : public QemuClient {
public:
    /* Constructs CameraQemuClient instance. */
    CameraQemuClient();

    /* Destructs CameraQemuClient instance. */
    ~CameraQemuClient();

    /****************************************************************************
     * Public API
     ***************************************************************************/

public:
    /* Queries camera connection.
     * Return:
     *  NO_ERROR on success, or an appropriate error status on failure.
     */
    status_t queryConnect();

    /* Queries camera disconnection.
     * Return:
     *  NO_ERROR on success, or an appropriate error status on failure.
     */
    status_t queryDisconnect();

    /* Queries camera to start capturing video.
     * Param:
     *  pixel_format - Pixel format that is used by the client to push video
     *      frames to the camera framework.
     *  width, height - Frame dimensions, requested by the framework.
     * Return:
     *  NO_ERROR on success, or an appropriate error status on failure.
     */
    status_t queryStart(uint32_t pixel_format, int width, int height);

    /* Queries camera to stop capturing video.
     * Return:
     *  NO_ERROR on success, or an appropriate error status on failure.
     */
    status_t queryStop();

    /* Queries camera for the next video frame.
     * Param:
     *  vframe, vframe_size - Define buffer, allocated to receive a video frame.
     *      Any of these parameters can be 0, indicating that the caller is
     *      interested only in preview frame.
     *  pframe, pframe_size - Define buffer, allocated to receive a preview frame.
     *      Any of these parameters can be 0, indicating that the caller is
     *      interested only in video frame.
     * Return:
     *  NO_ERROR on success, or an appropriate error status on failure.
     */
    status_t queryFrame(void* vframe,
                        void* pframe,
                        size_t vframe_size,
                        size_t pframe_size);

    /****************************************************************************
     * Names of the queries available for the emulated camera.
     ***************************************************************************/

private:
    /* Connect to the camera. */
    static const char mQueryConnect[];
    /* Disconnect from the camera. */
    static const char mQueryDisconnect[];
    /* Start video capturing. */
    static const char mQueryStart[];
    /* Stop video capturing. */
    static const char mQueryStop[];
    /* Query frame(s). */
    static const char mQueryFrame[];
};

}; /* namespace android */

#endif  /* HW_EMULATOR_CAMERA_QEMU_CLIENT_H */
