// Copyright (C) 2009 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#pragma version(1)
#pragma stateVertex(PVBackground)

#define RSID_POINTS 0

void dumpState() {

//    debugF("@@@@@ yrot: ", State->yRotation);

}

int main(int launchID) {

    int i;

    // Change the model matrix to account for the large model
    // and to do the necessary rotations.
    float mat1[16];
    float rads = ((float)startTimeMillis()) / 1000;
    float xrot = degf(-rads);
    float yrot = State->yRotation;
    float scale = 1.0/900.0;
    matrixLoadScale(mat1, scale, scale, scale);
    matrixRotate(mat1, yrot, 0.f, 1.f, 0.f);
    matrixRotate(mat1, xrot, 1.f, 0.f, 0.f);
    vpLoadModelMatrix(mat1);

    // Draw the cube. The default color will be used,
    // but we can also set the color here with the color()
    // function, or specify the color(s) as part of
    // the vertex data.
    uploadToBufferObject(NAMED_PointBuffer);
    drawSimpleMesh(NAMED_CubeMesh);

    return 1;
}
