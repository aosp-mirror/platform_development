/*
* Copyright 2006 Sony Computer Entertainment Inc.
*
* Licensed under the MIT Open Source License, for details please see license.txt or the website
* http://www.opensource.org/licenses/mit-license.php
*
*/

#ifndef COLLADA_CONDITIONER
#define COLLADA_CONDITIONER

#include <dae.h>
#include <dom/domConstants.h>
#include <dom/domCOLLADA.h>

class ColladaConditioner {

private:
    unsigned int getMaxOffset( domInputLocalOffset_Array &input_array );
    void createTrianglesFromPolylist( domMesh *thisMesh, domPolylist *thisPolylist );
    void createTrianglesFromPolygons( domMesh *thisMesh, domPolygons *thisPolygons );

public:
    bool triangulate(DAE *dae);
    bool triangulate(const char *inputFile);
    bool stripGeometry(DAE *dae);
    bool stripGeometry(const char *inputFile);
};

#endif //COLLADA_CONDITIONER
