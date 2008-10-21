//
// Copyright 2005 The Android Open Source Project
//
// Preferences file access.
//
#ifndef _SIM_PREFERENCES_H
#define _SIM_PREFERENCES_H

#include "tinyxml.h"

/*
 * This class provides access to a preferences file.  It's possible to
 * have more than one instance, though it's probably unwise to have multiple
 * objects for the same file on disk.
 *
 * All value are stored as strings.  The class doesn't really try to
 * enforce type safety, but it will complain if you try to do something
 * nonsensical (like convert "foo" to an integer).
 */
class Preferences {
public:
    Preferences(void) :
        mpDoc(NULL), mDirty(false)
        {}
    ~Preferences(void) {
        delete mpDoc;
    }

    /* load all preferences from a file */
    bool Load(const char* fileName);

    /* save all preferences to a file */
    bool Save(const char* fileName);

    /* create new preferences set (use when file does not exist) */
    bool Create(void);

    /*
     * Retrieve a value from the preferences database.
     *
     * These return "false" if the value was not found or could not be
     * converted to the expected type.  The value pointed to be the second
     * arg is guaranteed to be left undisturbed in this case.
     *
     * The value set by GetString(const char*, char**) will be set to
     * newly-allocated storage that must be freed with "delete[]".  This
     * is done instead of returning a const char* because it's unclear
     * what guarantees TinyXml makes wrt string lifetime (especially in
     * a multithreaded environment).
     */
    bool GetBool(const char* name, bool* pVal) const;
    bool GetInt(const char* name, int* pInt) const;
    bool GetDouble(const char* name, double* pDouble) const;
    bool GetString(const char* name, char** pVal) const;
    bool GetString(const char* name, wxString& str) const;

    /*
     * Set a value in the database.
     */
    void SetBool(const char* name, bool val);
    void SetInt(const char* name, int val);
    void SetDouble(const char* name, double val);
    void SetString(const char* name, const char* val);

    /*
     * Just test for existence.
     */
    bool Exists(const char* name) const;
    
    /*
     * Remove a <pref> from the config file.
     */
    bool RemovePref(const char* name);
    
    /*
     * Get the value of the "dirty" flag.
     */
    bool GetDirty(void) const { return mDirty; }

private:
    /* Implementation of getters */
    bool _GetBool(TiXmlElement* pElem, bool* pVal) const;
    bool _GetInt(TiXmlElement* pElem, int* pInt) const;
    bool _GetDouble(TiXmlElement* pElem, double* pDouble) const;
    bool _GetString(TiXmlElement* pElem, wxString& str) const;
    
    /* this can be used to generate some defaults */
    void SetDefaults(void);

    /* locate the named preference */
    TiXmlNode* _FindNode(const char* type, const char* name) const;
    TiXmlNode* FindPref(const char* str) const;
    /* like FindPref, but returns a TiXmlElement */
    TiXmlElement* FindPrefElement(const char* str) const;
    /* add a new preference entry */
    TiXmlElement* AddPref(const char* str);
    /* removes a node */
    bool _RemoveNode(TiXmlNode* pNode);
    
    TiXmlDocument*  mpDoc;
    bool            mDirty;
};

#endif // _SIM_PREFERENCES_H
