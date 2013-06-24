/*
 * Copyright (C) 2013 The Android Open Source Project
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
 *
 */

#include <stdio.h>
#include <stdlib.h>

#define ARRAY_LENGTH 10

int flag;

void clobber(int *array, int size) {
    /* Make sure it clobbers something. */
    array[-1] = 0x123;
    array[size] = 0x123;
}

int main(void) {
    int values[ARRAY_LENGTH];
    int *p = (int *) malloc(sizeof(int));
    *p = 10;

    while (!flag) {
        sleep(1);
    }

    /* Set a breakpint here: "b main.c:41" */
    clobber(values, ARRAY_LENGTH);
    printf("*p = %d\n", *p);
    free(p);

    return 0;
}
