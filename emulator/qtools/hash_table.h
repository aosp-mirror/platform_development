// Copyright 2006 The Android Open Source Project

#ifndef HASH_TABLE_H
#define HASH_TABLE_H

#include <string.h>
#include <inttypes.h>

template<class T>
class HashTable {
  public:
    HashTable(int size, T default_value = T());
    ~HashTable();

    typedef struct entry {
        entry    *next;
        char     *key;
        T        value;
    } entry_type;

    typedef T value_type;

    void         Update(const char *key, T value);
    bool         Remove(const char *key);
    T            Find(const char *key);
    entry_type*  GetFirst();
    entry_type*  GetNext();

  private:
    uint32_t     HashFunction(const char *key);

    int          size_;
    int          mask_;
    T            default_value_;
    entry_type   **table_;
    int          num_entries_;
    int          current_index_;
    entry_type   *current_ptr_;
};

template<class T>
HashTable<T>::HashTable(int size, T default_value)
{
    int pow2;

    // Round up size to a power of two
    for (pow2 = 2; pow2 < size; pow2 <<= 1)
        ;    // empty body

    size_ = pow2;
    mask_ = pow2 - 1;
    default_value_ = default_value;

    // Allocate a table of pointers and initialize them all to NULL.
    table_ = new entry_type*[size_];
    for (int ii = 0; ii < size_; ++ii)
        table_[ii] = NULL;
    num_entries_ = 0;
    current_index_ = 0;
    current_ptr_ = NULL;
}

template<class T>
HashTable<T>::~HashTable()
{
    for (int ii = 0; ii < size_; ++ii) {
        entry_type *ptr, *next;

        // Delete all the pointers in the chain at this table position.
        // Save the next pointer before deleting each entry so that we
        // don't dereference part of a deallocated object.
        for (ptr = table_[ii]; ptr; ptr = next) {
            next = ptr->next;
            delete[] ptr->key;
            delete ptr;
        }
    }
    delete[] table_;
}

// Professor Daniel J. Bernstein's hash function.  See
// http://www.partow.net/programming/hashfunctions/
template<class T>
uint32_t HashTable<T>::HashFunction(const char *key)
{
    uint32_t hash = 5381;

    int len = strlen(key);
    for(int ii = 0; ii < len; ++key, ++ii)
        hash = ((hash << 5) + hash) + *key;

    return hash;
}

template<class T>
void HashTable<T>::Update(const char *key, T value)
{
    // Hash the key to get the table position
    int len = strlen(key);
    int pos = HashFunction(key) & mask_;

    // Search the chain for a matching key
    for (entry_type *ptr = table_[pos]; ptr; ptr = ptr->next) {
        if (strcmp(ptr->key, key) == 0) {
            ptr->value = value;
            return;
        }
    }

    // Create a new hash entry and fill in the values
    entry_type *ptr = new entry_type;

    // Copy the string
    ptr->key = new char[len + 1];
    strcpy(ptr->key, key);
    ptr->value = value;

    // Insert the new entry at the beginning of the list
    ptr->next = table_[pos];
    table_[pos] = ptr;
    num_entries_ += 1;
}

template<class T>
bool HashTable<T>::Remove(const char *key)
{
    // Hash the key to get the table position
    int len = strlen(key);
    int pos = HashFunction(key) & mask_;

    // Search the chain for a matching key and keep track of the previous
    // element in the chain.
    entry_type *prev = NULL;
    for (entry_type *ptr = table_[pos]; ptr; prev = ptr, ptr = ptr->next) {
        if (strcmp(ptr->key, key) == 0) {
            if (prev == NULL) {
                table_[pos] = ptr->next;
            } else {
                prev->next = ptr->next;
            }
            delete ptr->key;
            delete ptr;
            return true;
        }
    }
    return false;
}

template<class T>
typename HashTable<T>::value_type HashTable<T>::Find(const char *key)
{
    // Hash the key to get the table position
    int pos = HashFunction(key) & mask_;

    // Search the chain for a matching key
    for (entry_type *ptr = table_[pos]; ptr; ptr = ptr->next) {
        if (strcmp(ptr->key, key) == 0)
            return ptr->value;
    }

    // If we get here, then we didn't find the key
    return default_value_;
}

template<class T>
typename HashTable<T>::entry_type* HashTable<T>::GetFirst()
{
    // Find the first non-NULL table entry.
    for (current_index_ = 0; current_index_ < size_; ++current_index_) {
        if (table_[current_index_])
            break;
    }

    // If there are no table entries, then return NULL.
    if (current_index_ == size_)
        return NULL;

    // Remember and return the current element.
    current_ptr_ = table_[current_index_];
    return current_ptr_;
}

template<class T>
typename HashTable<T>::entry_type* HashTable<T>::GetNext()
{
    // If we already iterated part way through the hash table, then continue
    // to the next element.
    if (current_ptr_) {
        current_ptr_ = current_ptr_->next;

        // If we are pointing to a valid element, then return it.
        if (current_ptr_)
            return current_ptr_;

        // Otherwise, start searching at the next table index.
        current_index_ += 1;
    }

    // Find the next non-NULL table entry.
    for (; current_index_ < size_; ++current_index_) {
        if (table_[current_index_])
            break;
    }

    // If there are no more non-NULL table entries, then return NULL.
    if (current_index_ == size_) {
        // Reset the current index so that we will start over from the
        // beginning on the next call to GetNext().
        current_index_ = 0;
        return NULL;
    }

    // Remember and return the current element.
    current_ptr_ = table_[current_index_];
    return current_ptr_;
}


#endif  // HASH_TABLE_H
