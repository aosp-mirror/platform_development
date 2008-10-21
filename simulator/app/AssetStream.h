//
// Copyright 2005 The Android Open Source Project
//
// Provide a wxInputStream subclass based on the Android Asset class.
// This is necessary because some wxWidgets functions require either a
// filename or a wxInputStream (e.g. wxImage).
//
#ifndef _SIM_ASSETSTREAM_H
#define _SIM_ASSETSTREAM_H

#include "wx/stream.h"
#include <utils/Asset.h>

/*
 * There is no sample code or concrete documentation about providing
 * input streams, but it seems straightforward.  The PNG loading code
 * uses the following:
 *  OnSysTell()
 *  OnSysSeek()
 *  Read()
 *
 * The AssetStream takes ownership of the Asset.
 */
class AssetStream : public wxInputStream {
public:
    AssetStream(android::Asset* pAsset)
        : mpAsset(pAsset)
        {}
    virtual ~AssetStream(void) {
        delete mpAsset;
    }

    virtual wxFileOffset GetLength() const {
        //printf("## GetLength --> %ld\n", (long) mpAsset->getLength());
        return mpAsset->getLength();
    }
    virtual size_t GetSize() const {
        //printf("## GetSize --> %ld\n", (long) mpAsset->getLength());
        return mpAsset->getLength();
    }
    virtual bool IsSeekable() const { return true; }

    virtual bool Eof() const {
        //printf("## Eof\n");
        return (mpAsset->seek(0, SEEK_CUR) == mpAsset->getLength());
    }

    virtual bool CanRead() const {
        //printf("## CanRead\n");
        return !Eof();
    }

    virtual wxInputStream& Read(void* buffer, size_t size) {
        OnSysRead(buffer, size);

        return *this;
    }

protected:
    /* read data, return number of bytes or 0 if EOF reached */
    virtual size_t OnSysRead(void* buffer, size_t size) {
        ssize_t actual = mpAsset->read(buffer, size);
        if (actual < 0) {
            // TODO: flag error
            actual = 0;
        }
        //printf("## OnSysRead(%p %u) --> %d\n", buffer, size, actual);
        return actual;
    }

    /* seek, using wxWidgets-defined values for "whence" */
    virtual wxFileOffset OnSysSeek(wxFileOffset seek, wxSeekMode mode) {
        int whence;
        off_t newPosn;

        if (mode == wxFromStart)
            whence = SEEK_SET;
        else if (mode == wxFromEnd)
            whence = SEEK_END;
        else
            whence = SEEK_CUR;
        newPosn = mpAsset->seek(seek, whence);
        //printf("## OnSysSeek(%ld %d) --> %ld\n",
        //    (long) seek, mode, (long) newPosn);
        if (newPosn == (off_t) -1)
            return wxInvalidOffset;
        else
            return newPosn;
    }

    virtual wxFileOffset OnSysTell() const {
        //printf("## OnSysTell() --> %ld\n", (long) mpAsset->seek(0, SEEK_CUR));
        return mpAsset->seek(0, SEEK_CUR);
    }

private:
    android::Asset*     mpAsset;

    DECLARE_NO_COPY_CLASS(AssetStream);     // private copy-ctor and op=
};

#endif // _SIM_ASSETSTREAM_H
