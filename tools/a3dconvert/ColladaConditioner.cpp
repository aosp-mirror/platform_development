/*
* Copyright 2006 Sony Computer Entertainment Inc.
*
* Licensed under the MIT Open Source License, for details please see license.txt or the website
* http://www.opensource.org/licenses/mit-license.php
*
*/

#include "ColladaConditioner.h"
unsigned int ColladaConditioner::getMaxOffset( domInputLocalOffset_Array &input_array ) {

    unsigned int maxOffset = 0;
    for ( unsigned int i = 0; i < input_array.getCount(); i++ ) {
        if ( input_array[i]->getOffset() > maxOffset ) {
            maxOffset = (unsigned int)input_array[i]->getOffset();
        }
    }
    return maxOffset;
}

void ColladaConditioner::createTrianglesFromPolylist( domMesh *thisMesh, domPolylist *thisPolylist ) {

    // Create a new <triangles> inside the mesh that has the same material as the <polylist>
    domTriangles *thisTriangles = (domTriangles *)thisMesh->createAndPlace("triangles");
    //thisTriangles->setCount( 0 );
    unsigned int triangles = 0;
    thisTriangles->setMaterial(thisPolylist->getMaterial());
    domP* p_triangles = (domP*)thisTriangles->createAndPlace("p");

    // Give the new <triangles> the same <_dae> and <parameters> as the old <polylist>
    for(int i=0; i<(int)(thisPolylist->getInput_array().getCount()); i++) {

        thisTriangles->placeElement( thisPolylist->getInput_array()[i]->clone() );
    }

    // Get the number of inputs and primitives for the polygons array
    int numberOfInputs = (int)getMaxOffset(thisPolylist->getInput_array()) + 1;
    int numberOfPrimitives = (int)(thisPolylist->getVcount()->getValue().getCount());

    unsigned int offset = 0;

    // Triangulate all the primitives, this generates all the triangles in a single <p> element
    for(int j = 0; j < numberOfPrimitives; j++) {

        int triangleCount = (int)thisPolylist->getVcount()->getValue()[j] -2;
        // Write out the primitives as triangles, just fan using the first element as the base
        int idx = numberOfInputs;
        for(int k = 0; k < triangleCount; k++) {
            // First vertex
            for(int l = 0; l < numberOfInputs; l++) {

                p_triangles->getValue().append(thisPolylist->getP()->getValue()[offset + l]);
            }
            // Second vertex
            for(int l = 0; l < numberOfInputs; l++) {

                p_triangles->getValue().append(thisPolylist->getP()->getValue()[offset + idx + l]);
            }
            // Third vertex
            idx += numberOfInputs;
            for(int l = 0; l < numberOfInputs; l++) {

                p_triangles->getValue().append(thisPolylist->getP()->getValue()[offset + idx + l]);
            }
            triangles++;
        }
        offset += (unsigned int)thisPolylist->getVcount()->getValue()[j] * numberOfInputs;
    }
    thisTriangles->setCount( triangles );

}

void ColladaConditioner::createTrianglesFromPolygons( domMesh *thisMesh, domPolygons *thisPolygons ) {

    // Create a new <triangles> inside the mesh that has the same material as the <polygons>
    domTriangles *thisTriangles = (domTriangles *)thisMesh->createAndPlace("triangles");
    thisTriangles->setCount( 0 );
    thisTriangles->setMaterial(thisPolygons->getMaterial());
    domP* p_triangles = (domP*)thisTriangles->createAndPlace("p");

    // Give the new <triangles> the same <_dae> and <parameters> as the old <polygons>
    for(int i=0; i<(int)(thisPolygons->getInput_array().getCount()); i++) {

        thisTriangles->placeElement( thisPolygons->getInput_array()[i]->clone() );
    }

    // Get the number of inputs and primitives for the polygons array
    int numberOfInputs = (int)getMaxOffset(thisPolygons->getInput_array()) +1;
    int numberOfPrimitives = (int)(thisPolygons->getP_array().getCount());

    // Triangulate all the primitives, this generates all the triangles in a single <p> element
    for(int j = 0; j < numberOfPrimitives; j++) {

        // Check the polygons for consistancy (some exported files have had the wrong number of indices)
        domP * thisPrimitive = thisPolygons->getP_array()[j];
        int elementCount = (int)(thisPrimitive->getValue().getCount());
        // Skip the invalid primitive
        if((elementCount % numberOfInputs) != 0) {
            continue;
        } else {
            int triangleCount = (elementCount/numberOfInputs)-2;
            // Write out the primitives as triangles, just fan using the first element as the base
            int idx = numberOfInputs;
            for(int k = 0; k < triangleCount; k++) {
                // First vertex
                for(int l = 0; l < numberOfInputs; l++) {

                    p_triangles->getValue().append(thisPrimitive->getValue()[l]);
                }
                // Second vertex
                for(int l = 0; l < numberOfInputs; l++) {

                    p_triangles->getValue().append(thisPrimitive->getValue()[idx + l]);
                }
                // Third vertex
                idx += numberOfInputs;
                for(int l = 0; l < numberOfInputs; l++) {

                    p_triangles->getValue().append(thisPrimitive->getValue()[idx + l]);
                }
                thisTriangles->setCount(thisTriangles->getCount()+1);
            }
        }
    }

}


bool ColladaConditioner::triangulate(DAE *dae) {

    int error = 0;

    // How many geometry elements are there?
    int geometryElementCount = (int)(dae->getDatabase()->getElementCount(NULL, "geometry" ));

    for(int currentGeometry = 0; currentGeometry < geometryElementCount; currentGeometry++) {

        // Find the next geometry element
        domGeometry *thisGeometry;
        //      error = _dae->getDatabase()->getElement((daeElement**)&thisGeometry,currentGeometry, NULL, "geometry");
        daeElement * element = 0;
        error = dae->getDatabase()->getElement(&element,currentGeometry, NULL, "geometry");
        thisGeometry = (domGeometry *) element;

        // Get the mesh out of the geometry
        domMesh *thisMesh = thisGeometry->getMesh();

        if (thisMesh == NULL){
            continue;
        }

        // Loop over all the polygon elements
        for(int currentPolygons = 0; currentPolygons < (int)(thisMesh->getPolygons_array().getCount()); currentPolygons++) {

            // Get the polygons out of the mesh
            // Always get index 0 because every pass through this loop deletes the <polygons> element as it finishes with it
            domPolygons *thisPolygons = thisMesh->getPolygons_array()[currentPolygons];
            createTrianglesFromPolygons( thisMesh, thisPolygons );
        }
        while (thisMesh->getPolygons_array().getCount() > 0) {

            domPolygons *thisPolygons = thisMesh->getPolygons_array().get(0);
            // Remove the polygons from the mesh
            thisMesh->removeChildElement(thisPolygons);
        }
        int polylistElementCount = (int)(thisMesh->getPolylist_array().getCount());
        for(int currentPolylist = 0; currentPolylist < polylistElementCount; currentPolylist++) {

            // Get the polylist out of the mesh
            // Always get index 0 because every pass through this loop deletes the <polygons> element as it finishes with it
            domPolylist *thisPolylist = thisMesh->getPolylist_array()[currentPolylist];
            createTrianglesFromPolylist( thisMesh, thisPolylist );
        }
        while (thisMesh->getPolylist_array().getCount() > 0) {

            domPolylist *thisPolylist = thisMesh->getPolylist_array().get(0);
            // Remove the polylist from the mesh
            thisMesh->removeChildElement(thisPolylist);
        }
    }
    return (error == 0);
}

bool ColladaConditioner::triangulate(const char *inputFile) {

    DAE dae;
    bool convertSuceeded = true;
    domCOLLADA* root = dae.open(inputFile);

    if (!root) {
        printf("Failed to read file %s.\n", inputFile);
        return false;
    }

    convertSuceeded = triangulate(&dae);

    dae.writeAll();
    if(!convertSuceeded) {
        printf("Encountered errors\n");
    }

    return convertSuceeded;
}

bool ColladaConditioner::stripGeometry(DAE *dae) {
    bool convertSuceeded = true;
    int geometryElementCount = (int)(dae->getDatabase()->getElementCount(NULL,
                                                                         "library_geometries" ));

    for(int currentGeometry = 0; currentGeometry < geometryElementCount; currentGeometry++) {

        daeElement * element = 0;
        int error = dae->getDatabase()->getElement(&element, currentGeometry,
                                                   NULL, "library_geometries");
        daeBool removed = daeElement::removeFromParent(element);
        convertSuceeded = convertSuceeded && removed;
    }
    return convertSuceeded;
}

bool ColladaConditioner::stripGeometry(const char *inputFile) {
    DAE dae;
    bool convertSuceeded = true;
    domCOLLADA* root = dae.open(inputFile);

    if (!root) {
        printf("Failed to read file %s.\n", inputFile);
        return false;
    }

    stripGeometry(&dae);

    dae.writeAll();
    if(!convertSuceeded) {
        printf("Encountered errors\n");
    }

    return convertSuceeded;
}
