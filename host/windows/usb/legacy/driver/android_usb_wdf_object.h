/*
 * Copyright (C) 2006 The Android Open Source Project
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

#ifndef ANDROID_USB_WDF_OBJECT_H__
#define ANDROID_USB_WDF_OBJECT_H__
/** \file
  This file consists of declaration of a class AndroidUsbWdfObject that
  encapsulates a basic extension to all KMDF objects. Currently, device and
  file object extensions ared derived from it.
*/

/** AndroidUsbWdfObject class encapsulates a basic extension to all KMDF
  objects. Currently, device and file object extensions ared derived from it.
  Instances of this and derived classes must be allocated from NonPagedPool.
*/
class AndroidUsbWdfObject {
 public:
  /** \brief Constructs the object.

    @param obj_type[in] Type of the object that this wrapper represents.
    This method must be called at low IRQL.
  */
  AndroidUsbWdfObject(AndroidUsbWdfObjectType obj_type);

  /** \brief Destructs the object.

    This method can be called at any IRQL.
  */
  virtual ~AndroidUsbWdfObject();

  /** \brief Initializes object attributes for new KMDF object.

    Each KMDF extension object must perform attribute initializations in order
    to register an extension with KMDF framework. Since all our extensions are
    derived from the base AndroidUsbWdfObject we use a single WDF object
    extension context for all KMDF objects that we extend. So we can initialize
    and register our context extension structure here. Note that object
    attributes for file object wrappers are initialized globaly, when device
    object is created. So file object extensions must not call this method.
    This method must be called at low IRQL.
    @param wdf_obj_attr[out] Object attributes to initialize.
    @param parent[in] Parent object for this object. Can be NULL.
    @return STATUS_SUCCESS on success or an appropriate error code.
  */
  virtual NTSTATUS InitObjectAttributes(PWDF_OBJECT_ATTRIBUTES wdf_obj_attr,
                                        WDFOBJECT parent);

  /** \brief Initializes context for this extension

    This method initializes AndroidUsbWdfObjectContext structure that KMDF
    allocated for the object that is being extended with this class.
    InitObjectAttributes method must be called prior to the call to this
    method. Besides, before calling this method, instance of this class must
    be already attached to the KMDF object it represents. Otherwise this
    method will fail with STATUS_INTERNAL_ERROR.
    This method must be called at low IRQL.
    @return STATUS_SUCCESS on success or an appropriate error code
  */
  virtual NTSTATUS InitializeContext();


 protected:
  /** \brief Returns syncronisation scope for this extension type.

    This method is called from InitObjectAttributes method to specify what
    type of synchronization is required for instances of this type. By
    default we return WdfSynchronizationScopeNone which makes KMDF not
    to synchronize access to this type of object.
    This method can be called at IRQL <= DISPATCH_LEVEL.
  */
  virtual WDF_SYNCHRONIZATION_SCOPE GetWdfSynchronizationScope();

  /** \brief Handler for cleanup event fired for associated KMDF object.

    The framework calls this callback function when either the framework or a
    driver attempts to delete the object.
    This method can be called at IRQL <= DISPATCH_LEVEL.
  */
  virtual void OnEvtCleanupCallback();

  /** \brief Handler for destroy callback

    The framework calls the EvtDestroyCallback callback function after the
    object's reference count has been decremented to zero. The framework
    deletes the object immediately after the EvtDestroyCallback callback
    function returns.
    This callback can be called at IRQL <= DISPATCH_LEVEL.
  */
  virtual void OnEvtDestroyCallback();

  /** \brief Removes driver's references on an object so it can be deleted.

    The framework calls the callback function when either the framework or a
    driver attempts to delete the object.
    This callback can be called at IRQL <= DISPATCH_LEVEL.
    @param wdf_obj[in] A handle to a framework object this class wraps.
  */
  static void EvtCleanupCallbackEntry(WDFOBJECT wdf_obj);

  /** \brief Called when framework object is being deleted

    The framework calls the EvtDestroyCallback callback function after the
    object's reference count has been decremented to zero. The framework
    deletes the object immediately after the EvtDestroyCallback callback
    function returns.
    This callback can be called at IRQL <= DISPATCH_LEVEL.
    @param wdf_obj[in] A handle to a framework object this class wraps.
  */
  static void EvtDestroyCallbackEntry(WDFOBJECT wdf_obj);

 public:

  /// Gets KMDF object extended with this instance
  __forceinline WDFOBJECT wdf_object() const {
    return wdf_object_;
  }

  /// Sets KMDF object associated with this extension
  __forceinline void set_wdf_object(WDFOBJECT wdf_obj) {
    ASSERT(NULL == wdf_object_);
    wdf_object_ = wdf_obj;
  }

  /// Gets KMDF object type for this extension
  __forceinline AndroidUsbWdfObjectType object_type() const {
    return object_type_;
  }

  /** \brief Checks if this extension represends KMDF object of the given type

    @param obj_type[in] Object type to check
    @return true if this wrapper represents object of that type and
            false otherwise.
  */
  __forceinline Is(AndroidUsbWdfObjectType obj_type) const {
    return (obj_type == object_type());
  }

  /// Checks if extension is attached to a KMDF object
  __forceinline bool IsAttached() const {
    return (NULL != wdf_object());
  }

 protected:
  /// KMDF object that is extended with this instance
  WDFOBJECT               wdf_object_;

  /// KMDF object type for this extension
  AndroidUsbWdfObjectType object_type_;
};

/** \brief Gets our extension for the given KMDF object

  This method can be called at any IRQL
  @param wdf_obj[in] KMDF handle describing an object
  @return Instance of AndroidUsbWdfObject associated with this object or NULL
          if association is not found.
*/
__forceinline AndroidUsbWdfObject* GetAndroidUsbWdfObjectFromHandle(
    WDFOBJECT wdf_obj) {
  ASSERT(NULL != wdf_obj);
  if (NULL != wdf_obj) {
    AndroidUsbWdfObjectContext* context =
      GetAndroidUsbWdfObjectContext(wdf_obj);
    ASSERT((NULL != context) && (NULL != context->wdf_object_ext) &&
           (context->wdf_object_ext->Is(context->object_type)));
    if ((NULL != context) && (NULL != context->wdf_object_ext) &&
        context->wdf_object_ext->Is(context->object_type)) {
      return context->wdf_object_ext;
    }
  }
  return NULL;
}

#endif  // ANDROID_USB_WDF_OBJECT_H__
