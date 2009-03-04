//
// Copyright 2005 The Android Open Source Project
//
// Preferences file access.
//

// For compilers that support precompilation, include "wx/wx.h".
#include "wx/wxprec.h"
// Otherwise, include all standard headers
#ifndef WX_PRECOMP
//# include "wx/wx.h"
# include "wx/string.h"
#endif

#include "Preferences.h"

#include "utils.h"
#include "tinyxml.h"

static const char* kName = "name";
static const char* kValue = "value";


/*
 * Load from a file.
 */
bool Preferences::Load(const char* fileName)
{
    assert(fileName != NULL);
    printf("SimPref: reading preferences file '%s'\n", fileName);

    // throw out any existing stuff
    delete mpDoc;

    mpDoc = new TiXmlDocument;
    if (mpDoc == NULL)
        return false;

    if (!mpDoc->LoadFile(fileName)) {
        fprintf(stderr, "SimPref: ERROR: failed loading '%s'\n", fileName);
        if (mpDoc->ErrorRow() != 0)
            fprintf(stderr, "    XML: %s (row=%d col=%d)\n",
                mpDoc->ErrorDesc(), mpDoc->ErrorRow(), mpDoc->ErrorCol());
        else
            fprintf(stderr, "    XML: %s\n", mpDoc->ErrorDesc());
        goto fail;
    }

    TiXmlNode* pPrefs;
    pPrefs = mpDoc->FirstChild("prefs");
    if (pPrefs == NULL) {
        fprintf(stderr, "SimPref: ERROR: could not find <prefs> in '%s'\n",
            fileName);
        goto fail;
    }

    // set defaults for anything we haven't set explicitly
    SetDefaults();

    return true;

fail:
    delete mpDoc;
    mpDoc = NULL;
    return false;
}

/*
 * Save to a file.
 */
bool Preferences::Save(const char* fileName)
{
    assert(fileName != NULL);

    if (mpDoc == NULL)
        return false;

    if (!mpDoc->SaveFile(fileName)) {
        fprintf(stderr, "SimPref: ERROR: failed saving '%s': %s\n",
            fileName, mpDoc->ErrorDesc());
        return false;
    }

    mDirty = false;

    return true;
}

/*
 * Create an empty collection of preferences.
 */
bool Preferences::Create(void)
{
    static const char* docBase =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
        "<!-- Android device simulator preferences -->\n"
        "<!-- This file is updated by the simulator -->\n"
        "<prefs>\n"
        "</prefs>\n";

    // throw out any existing stuff
    delete mpDoc;

    // alloc and initialize
    mpDoc = new TiXmlDocument;
    if (mpDoc == NULL)
        return false;

    if (!mpDoc->Parse(docBase)) {
        fprintf(stderr, "SimPref: bad docBase: %s\n", mpDoc->ErrorDesc());
        return false;
    }

    SetDefaults();
    mDirty = true;      // should already be, mbut make sure
    return true;
}

/*
 * Add default values to XML doc.
 *
 * This isn't strictly necessary, because the functions that are interested
 * in the preferences can set appropriate defaults themselves when the
 * "get" function returns "false".  However, in some cases a preference
 * can be interesting to more than one function, and you either have to
 * cut & paste the default value or write a "get default for xxx" function.
 *
 * We want this to work even if they already have an older config file, so
 * this only sets values that don't already exist.
 */
void Preferences::SetDefaults(void)
{
    /* table of default values */
    static const struct {
        const char* type;
        const char* name;
        const char* value;
    } kDefault[] = {
        { "pref",           "auto-power-on",        "true" },
        { "pref",           "debug",                "false" },
        { "pref",           "valgrind",             "false" },
        { "pref",           "check-jni",            "true" },
        { "pref",           "enable-sound",         "true" },
        { "pref",           "enable-fake-camera",   "true" },
        { "pref",           "java-vm",              "Dalvik" },
        /* goobuntu dapper needed LD_ASSUME_KERNEL or gdb choked badly */
        { "pref",           "ld-assume-kernel",     "" /*2.4.19*/ },
        { "pref",           "launch-command",
            "xterm -geom 80x60+10+10 -sb -title Simulator -e" },
        { "pref",           "launch-wrapper-args",  "-wait" },
    };
    TiXmlNode* pPrefs;

    assert(mpDoc != NULL);

    pPrefs = mpDoc->FirstChild("prefs");

    /*
     * Look up the name.  If it doesn't exist, add it.
     */
    for (int i = 0; i < NELEM(kDefault); i++) {
        TiXmlNode* pNode = _FindNode(kDefault[i].type, kDefault[i].name);

        if (pNode == NULL) {
            TiXmlElement elem(kDefault[i].type);
            elem.SetAttribute(kName, kDefault[i].name);
            elem.SetAttribute(kValue, kDefault[i].value);
            pPrefs->InsertEndChild(elem);

            printf("SimPref: added default <%s> '%s'='%s'\n",
                kDefault[i].type, kDefault[i].name, kDefault[i].value);
        } else {
            printf("SimPref: found existing <%s> '%s'\n", 
                kDefault[i].type, kDefault[i].name);
        }
    }
}

static TiXmlNode* get_next_node(TiXmlNode* pNode)
{
  if (!pNode->NoChildren())
  {
      pNode = pNode->FirstChild();
  }
  else if (pNode->NoChildren() && 
           (pNode->NextSibling() == NULL))
  {
      pNode = pNode->Parent()->NextSibling();
  }
  else
  {
      pNode = pNode->NextSibling();
  }
  return pNode;
}

/*
 * Returns the node with element type and name specified
 *
 * WARNING: this searches through the tree and returns the first matching
 * node.
 */
TiXmlNode* Preferences::_FindNode(const char* type, const char* str) const
{
    assert((type != NULL) && (str != NULL));
    TiXmlNode* pRoot;
    TiXmlNode* pNode;

    pRoot = mpDoc->FirstChild("prefs");
    assert(pRoot != NULL);

    for (pNode = pRoot->FirstChild(); pNode != NULL;)
    {
        if (pNode->Type() != TiXmlNode::ELEMENT ||
            strcasecmp(pNode->Value(), type) != 0)
        {
            pNode = get_next_node(pNode);
            continue;
        }

        TiXmlElement* pElem = pNode->ToElement();
        assert(pElem != NULL);

        const char* name = pElem->Attribute(kName);

        /* 1. If the name is blank, something is wrong with the config file
         * 2. If the name matches the passed in string, we found the node
         * 3. If the node has children, descend another level
         * 4. If there are no children and no siblings of the node, go up a level
         * 5. Otherwise, grab the next sibling
         */
        if (name == NULL)
        {
            fprintf(stderr, "WARNING: found <%s> without name\n", type);
            continue;
        }
        else if (strcasecmp(name, str) == 0)
        {
            return pNode;
        }
        else 
        {
            pNode = get_next_node(pNode);
        }
    }

    return NULL;
}

/*
 * Locate the specified preference.
 */
TiXmlNode* Preferences::FindPref(const char* str) const
{
    TiXmlNode* pNode = _FindNode("pref", str);
    return pNode;
}

/*
 * Like FindPref(), but returns a TiXmlElement.
 */
TiXmlElement* Preferences::FindPrefElement(const char* str) const
{
    TiXmlNode* pNode;

    pNode = FindPref(str);
    if (pNode != NULL)
        return pNode->ToElement();
    return NULL;
}

/*
 * Add a new preference entry with a blank entry for value.  Returns a
 * pointer to the new element.
 */
TiXmlElement* Preferences::AddPref(const char* str)
{
    assert(FindPref(str) == NULL);

    TiXmlNode* pPrefs;

    pPrefs = mpDoc->FirstChild("prefs");
    assert(pPrefs != NULL);

    TiXmlElement elem("pref");
    elem.SetAttribute(kName, str);
    elem.SetAttribute(kValue, "");
    pPrefs->InsertEndChild(elem);

    TiXmlNode* pNewPref = FindPref(str);
    return pNewPref->ToElement();
}

/*
 * Remove a node from the tree
 */
bool Preferences::_RemoveNode(TiXmlNode* pNode)
{
    if (pNode == NULL)
        return false;

    TiXmlNode* pParent = pNode->Parent();
    if (pParent == NULL)
        return false;

    pParent->RemoveChild(pNode);
    mDirty = true;
    return true;
}

/*
 * Remove a preference entry.
 */
bool Preferences::RemovePref(const char* delName)
{
    return _RemoveNode(FindPref(delName));
}

/*
 * Test for existence.
 */
bool Preferences::Exists(const char* name) const
{
    TiXmlElement* pElem = FindPrefElement(name);
    return (pElem != NULL);
}

/*
 * Internal implemenations for getting values 
 */
bool Preferences::_GetBool(TiXmlElement* pElem, bool* pVal) const
{
    if (pElem != NULL) 
    {
        const char* str = pElem->Attribute(kValue);
        if (str != NULL) 
        {
            if (strcasecmp(str, "true") == 0)
                *pVal = true;
            else if (strcasecmp(str, "false") == 0)
                *pVal = false;
            else 
            {
                printf("SimPref: evaluating as bool name='%s' val='%s'\n",
                pElem->Attribute(kName), str);
                return false;
            }
            return true;
        }
    }
    return false;
}

bool Preferences::_GetInt(TiXmlElement* pElem, int* pInt) const
{
    int val;
    if (pElem != NULL && pElem->Attribute(kValue, &val) != NULL) {
        *pInt = val;
        return true;
    }
    return false;
}

bool Preferences::_GetDouble(TiXmlElement* pElem, double* pDouble) const
{
    double val;
    if (pElem != NULL && pElem->Attribute(kValue, &val) != NULL) {
        *pDouble = val;
        return true;
    }
    return false;
}

bool Preferences::_GetString(TiXmlElement* pElem, wxString& str) const
{
    const char* val;
    if (pElem != NULL) {
        val = pElem->Attribute(kValue);
        if (val != NULL) {
            str = wxString::FromAscii(val);
            return true;
        }
    }
    return false;
}

/*
 * Get a value.  Do not disturb "*pVal" unless we have something to return.
 */
bool Preferences::GetBool(const char* name, bool* pVal) const
{
    return _GetBool(FindPrefElement(name), pVal);
}

bool Preferences::GetInt(const char* name, int* pInt) const
{
    return _GetInt(FindPrefElement(name), pInt);
}

bool Preferences::GetDouble(const char* name, double* pDouble) const
{
    return _GetDouble(FindPrefElement(name), pDouble);
}

bool Preferences::GetString(const char* name, char** pVal) const
{
    wxString str = wxString::FromAscii(*pVal);
    if (_GetString(FindPrefElement(name), str))
    {
        *pVal = android::strdupNew(str.ToAscii());
        return true;
    }
    return false;   
}

bool Preferences::GetString(const char* name, wxString& str) const
{
    return _GetString(FindPrefElement(name), str);
}

/*
 * Set a value.  If the preference already exists, and the value hasn't
 * changed, don't do anything.  This avoids setting the "dirty" flag
 * unnecessarily.
 */
void Preferences::SetBool(const char* name, bool val)
{
    bool oldVal;
    if (GetBool(name, &oldVal) && val == oldVal)
        return;

    SetString(name, val ? "true" : "false");
    mDirty = true;
}

void Preferences::SetInt(const char* name, int val)
{
    int oldVal;
    if (GetInt(name, &oldVal) && val == oldVal)
        return;

    TiXmlElement* pElem = FindPrefElement(name);
    if (pElem == NULL)
        pElem = AddPref(name);
    pElem->SetAttribute(kValue, val);
    mDirty = true;
}

void Preferences::SetDouble(const char* name, double val)
{
    double oldVal;
    if (GetDouble(name, &oldVal) && val == oldVal)
        return;

    TiXmlElement* pElem = FindPrefElement(name);
    if (pElem == NULL)
        pElem = AddPref(name);
    pElem->SetDoubleAttribute(kValue, val);
    mDirty = true;
}

void Preferences::SetString(const char* name, const char* val)
{
    wxString oldVal;
    if (GetString(name, /*ref*/oldVal) && strcmp(oldVal.ToAscii(), val) == 0)
        return;

    TiXmlElement* pElem = FindPrefElement(name);
    if (pElem == NULL)
        pElem = AddPref(name);
    pElem->SetAttribute(kValue, val);
    mDirty = true;
}


