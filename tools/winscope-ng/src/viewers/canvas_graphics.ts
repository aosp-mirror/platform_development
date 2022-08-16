/*
 * Copyright (C) 2022 The Android Open Source Project
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
import { Rectangle } from "viewers/viewer_surface_flinger/ui_data";
import * as THREE from "three";
import { CSS2DRenderer, CSS2DObject } from "three/examples/jsm/renderers/CSS2DRenderer";

export class CanvasGraphics {
  constructor() {
    //set up camera
    const left = -this.cameraHalfWidth,
      right = this.cameraHalfWidth,
      top = this.cameraHalfHeight,
      bottom = -this.cameraHalfHeight,
      near = 0.001,
      far = 100;
    this.camera = new THREE.OrthographicCamera(
      left,right,top,bottom,near,far
    );
  }

  initialise(canvas: HTMLCanvasElement) {
    // initialise canvas
    this.canvas = canvas;
  }

  refreshCanvas() {
    //set canvas size
    this.canvas!.style.width = "100%";
    this.canvas!.style.height = "40rem";

    // TODO: click and drag rotation control
    this.camera.position.set(this.xyCameraPos, this.xyCameraPos, 6);
    this.camera.lookAt(0, 0, 0);
    this.camera.zoom = this.camZoom;
    this.camera.updateProjectionMatrix();

    // scene
    const scene = new THREE.Scene();

    // renderers
    const renderer = new THREE.WebGLRenderer({
      antialias: true,
      canvas: this.canvas,
      alpha: true
    });
    let labelRenderer: CSS2DRenderer;
    if (document.querySelector("#labels-canvas")) {
      labelRenderer = new CSS2DRenderer({
        element: document.querySelector("#labels-canvas")! as HTMLElement
      });
    } else {
      labelRenderer = new CSS2DRenderer();
      labelRenderer.domElement.style.position = "absolute";
      labelRenderer.domElement.style.top = "0px";
      labelRenderer.domElement.style.width = "100%";
      labelRenderer.domElement.style.height = "40rem";
      labelRenderer.domElement.id = "labels-canvas";
      labelRenderer.domElement.style.pointerEvents = "none";
      document.querySelector(".canvas-container")?.appendChild(labelRenderer.domElement);
    }

    // set various factors for shading and shifting
    const visibleDarkFactor = 0, nonVisibleDarkFactor = 0, rectCounter = 0;
    const numberOfRects = this.rects.length;
    const numberOfVisibleRects = this.rects.filter(rect => rect.isVisible).length;
    const numberOfDisplayRects = this.rects.filter(rect => rect.isDisplay).length;

    const zShift = numberOfRects*this.layerSeparation;
    let xShift = 0, yShift = 3.25, labelYShift = 0;

    if (this.isLandscape) {
      xShift = 1;
      yShift = 1.5;
      labelYShift = 1.25;
    }

    const lowestY = Math.min(...this.rects.map(rect => {
      const y = rect.topLeft.y - rect.height + this.lowestYShift;
      if (this.isLandscape) {
        if (y<0) {
          return 0;
        } else if (y > 2) {
          return 2;
        }
      } else if (y > -1) {
        return -1;
      }
      return y;
    })) - labelYShift;

    this.drawScene(
      rectCounter,
      numberOfVisibleRects,
      visibleDarkFactor,
      numberOfDisplayRects,
      nonVisibleDarkFactor,
      numberOfRects,
      scene,
      xShift,
      yShift,
      zShift,
      lowestY
    );

    // const axesHelper = new THREE.AxesHelper(1);
    // const gridHelper = new THREE.GridHelper(5);
    // scene.add(axesHelper, gridHelper)

    renderer.setSize(this.canvas!.clientWidth, this.canvas!.clientHeight);
    renderer.setPixelRatio(window.devicePixelRatio);
    renderer.render(scene, this.camera);

    labelRenderer.setSize(this.canvas!.clientWidth, this.canvas!.clientHeight);
    labelRenderer.render(scene, this.camera);
  }

  private drawScene(
    rectCounter: number,
    visibleRects: number,
    visibleDarkFactor:number,
    displayRects: number,
    nonVisibleDarkFactor: number,
    numberOfRects: number,
    scene: THREE.Scene,
    xShift: number,
    yShift: number,
    zShift: number,
    lowestY: number
  ) {
    this.targetObjects = [];
    this.rects.forEach(rect => {
      const visibleViewInvisibleRect = this.visibleView && !rect.isVisible;
      const xrayViewNoVirtualDisplaysVirtualRect = !this.visibleView && !this.showVirtualDisplays && rect.isDisplay && rect.isVirtual;
      if (visibleViewInvisibleRect || xrayViewNoVirtualDisplaysVirtualRect) {
        rectCounter++;
        return;
      }

      //set colour mapping
      let planeColor;
      if (this.highlighted === `${rect.id}`) {
        planeColor = this.colorMapping("highlight", numberOfRects, 0);
      } else if (rect.isVisible) {
        planeColor = this.colorMapping("green", visibleRects, visibleDarkFactor);
        visibleDarkFactor++;
      } else if (rect.isDisplay) {
        planeColor = this.colorMapping("grey", displayRects, nonVisibleDarkFactor);
        nonVisibleDarkFactor++;
      } else {
        planeColor = this.colorMapping("unknown", numberOfRects, 0);
      }

      //set plane geometry and material
      const geometry = new THREE.PlaneGeometry(rect.width, rect.height);
      const planeRect = this.setPlaneMaterial(rect, geometry, planeColor, xShift, yShift, zShift);
      scene.add(planeRect);
      zShift -= this.layerSeparation;

      // bolder edges of each plane if in x-ray view
      if (!this.visibleView) {
        const edgeSegments = this.setEdgeMaterial(planeRect, geometry);
        scene.add(edgeSegments);
      }

      // label circular marker
      const circle = this.setCircleMaterial(planeRect, rect);
      scene.add(circle);
      this.targetObjects.push(planeRect);

      // label line
      const [line, rectLabel] = this.createLabel(rect, circle, lowestY, rectCounter);
      scene.add(line);
      scene.add(rectLabel);
      rectCounter++;
    });
  }

  private setPlaneMaterial(
    rect: Rectangle,
    geometry: THREE.PlaneGeometry,
    color: THREE.Color,
    xShift: number,
    yShift: number,
    zShift: number
  ) {
    const planeRect = new THREE.Mesh(
      geometry,
      new THREE.MeshBasicMaterial({
        color: color,
        opacity: this.visibleView ? 1 : 0.75,
        transparent: true,
      }));
    planeRect.position.y = rect.topLeft.y - rect.height/2 + yShift;
    planeRect.position.x = rect.topLeft.x + rect.width/2 - xShift;
    planeRect.position.z =  zShift;
    planeRect.name = `${rect.id}`;
    return planeRect;
  }

  private setEdgeMaterial(planeRect: THREE.Mesh, geometry: THREE.PlaneGeometry) {
    const edgeColor = 0x000000;
    const edgeGeo = new THREE.EdgesGeometry(geometry);
    const edgeMaterial = new THREE.LineBasicMaterial({color: edgeColor, linewidth: 1});
    const edgeSegments = new THREE.LineSegments(
      edgeGeo, edgeMaterial
    );
    edgeSegments.position.set(planeRect.position.x, planeRect.position.y, planeRect.position.z);
    return edgeSegments;
  }

  private setCircleMaterial(planeRect: THREE.Mesh, rect: Rectangle) {
    const labelCircle = new THREE.CircleGeometry(0.02, 200);
    const circleMaterial = new THREE.MeshBasicMaterial({ color: 0x000000 });
    const circle = new THREE.Mesh(labelCircle, circleMaterial);
    circle.position.set(
      planeRect.position.x + rect.width/2 - 0.05,
      planeRect.position.y,
      planeRect.position.z + 0.05
    );
    circle.rotateY(THREE.MathUtils.degToRad(30));
    return circle;
  }

  private createLabel(rect: Rectangle, circle: THREE.Mesh, lowestY: number, rectCounter: number):
    [THREE.Line, CSS2DObject] {
    const labelText = this.shortenText(rect.label);
    const isGrey = !this.visibleView && !rect.isVisible;
    let cornerPos, endPos;
    const labelYSeparation = 0.3;
    if (this.isLandscape) {
      cornerPos = new THREE.Vector3(
        circle.position.x, lowestY - 0.5 - rectCounter*labelYSeparation, circle.position.z
      );
    } else {
      cornerPos = new THREE.Vector3(
        circle.position.x, lowestY + 0.5 - rectCounter*labelYSeparation, circle.position.z
      );
    }

    const linePoints = [circle.position, cornerPos];
    if (this.isLandscape && cornerPos.x > 0 || !this.isLandscape) {
      endPos = new THREE.Vector3(cornerPos.x - 1, cornerPos.y - this.labelShift, cornerPos.z);
    } else {
      endPos = cornerPos;
    }
    linePoints.push(endPos);

    //add rectangle label
    document.querySelector(`.label-${rectCounter}`)?.remove();
    const rectLabelDiv: HTMLElement = document.createElement("div");
    this.labelElements.push(rectLabelDiv);
    rectLabelDiv.className = `label-${rectCounter}`;
    rectLabelDiv.textContent = labelText;
    rectLabelDiv.style.fontSize = "10px";
    if (isGrey) {
      rectLabelDiv.style.color = "grey";
    }
    const rectLabel = new CSS2DObject(rectLabelDiv);
    rectLabel.name = rect.label;

    const textCanvas = document.createElement("canvas");
    const labelContext = textCanvas.getContext("2d");
    let labelWidth = 0;
    if (labelContext?.font) {
      labelContext.font = rectLabelDiv.style.font;
      labelWidth = labelContext?.measureText(labelText).width;
    }
    textCanvas.remove();

    if (this.isLandscape && endPos.x < 0) {
      rectLabel.position.set(
        endPos.x + 0.6, endPos.y - 0.15, endPos.z - 0.6
      );
    } else {
      rectLabel.position.set(
        endPos.x - labelWidth * this.labelXFactor, endPos.y - this.labelShift * labelWidth * this.labelXFactor, endPos.z
      );
    }

    const lineGeo = new THREE.BufferGeometry().setFromPoints(linePoints);
    const lineMaterial = new THREE.LineBasicMaterial({color: isGrey ? 0x808080 : 0x000000});
    const line = new THREE.Line(lineGeo, lineMaterial);

    return [line, rectLabel];
  }

  getCamera() {
    return this.camera;
  }

  getTargetObjects() {
    return this.targetObjects;
  }

  getLayerSeparation() {
    return this.layerSeparation;
  }

  getVisibleView() {
    return this.visibleView;
  }

  getXyCameraPos() {
    return this.xyCameraPos;
  }

  getShowVirtualDisplays() {
    return this.showVirtualDisplays;
  }

  updateLayerSeparation(userInput: number) {
    this.layerSeparation = userInput;
  }

  updateRotation(userInput: number) {
    this.xyCameraPos = userInput;
    this.camZoom = userInput/4 * 0.2 + 0.9;
    this.labelShift = userInput/4 * this.maxLabelShift;
    this.lowestYShift = userInput/4 + 2;
  }

  updateHighlighted(highlighted: string) {
    this.highlighted = highlighted;
  }

  updateRects(rects: Rectangle[]) {
    this.rects = rects;
  }

  updateIsLandscape(isLandscape: boolean) {
    this.isLandscape = isLandscape;
  }

  updateVisibleView(visible: boolean) {
    this.visibleView = visible;
  }

  updateVirtualDisplays(show: boolean) {
    this.showVirtualDisplays = show;
  }

  clearLabelElements() {
    this.labelElements.forEach(el => el.remove());
  }

  updateZoom(isZoomIn: boolean) {
    if (isZoomIn && this.camZoom < 2) {
      this.camZoom += this.camZoomFactor * 1.5;
    } else if (!isZoomIn && this.camZoom > 0.5) {
      this.camZoom -= this.camZoomFactor * 1.5;
    }
  }

  colorMapping(scale: string, numberOfRects: number, darkFactor:number): THREE.Color {
    if (scale === "highlight") {
      return new THREE.Color(0xD2E3FC);
    } else if (scale === "grey") {
      // darkness of grey rect depends on z order - darkest 64, lightest 128
      //Separate RGB values between 0 and 1
      const lower = 120;
      const upper = 220;
      const darkness = ((upper-lower)*(numberOfRects-darkFactor)/numberOfRects + lower)/255;
      return new THREE.Color(darkness, darkness, darkness);
    } else if (scale === "green") {
      // darkness of green rect depends on z order
      //Separate RGB values between 0 and 1
      const red = ((200-45)*(numberOfRects-darkFactor)/numberOfRects + 45)/255;
      const green = ((232-182)*(numberOfRects-darkFactor)/numberOfRects + 182)/255;
      const blue = ((183-44)*(numberOfRects-darkFactor)/numberOfRects + 44)/255;
      return new THREE.Color(red, green, blue);
    } else {
      return new THREE.Color(0, 0, 0);
    }
  }

  shortenText(text: string): string {
    if (text.length > 40) {
      text = text.slice(0, 40);
    }
    return text;
  }

  // dynamic scaling and canvas variables
  readonly cameraHalfWidth = 2.8;
  readonly cameraHalfHeight = 3.2;
  private readonly maxLabelShift = 0.305;
  private readonly labelXFactor = 0.008;
  private lowestYShift = 3;
  private camZoom = 1.1;
  private camZoomFactor = 0.1;
  private labelShift = this.maxLabelShift;
  private highlighted = "";
  private visibleView = false;
  private isLandscape = false;
  private showVirtualDisplays = false;
  private layerSeparation = 0.4;
  private xyCameraPos = 4;
  private camera: THREE.OrthographicCamera;
  private rects: Rectangle[] = [];
  private labelElements: HTMLElement[] = [];
  private targetObjects: any[] = [];
  private canvas?: HTMLCanvasElement;
}
