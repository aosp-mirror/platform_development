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
import { Rectangle } from "viewers/common/rectangle";
import * as THREE from "three";
import { CSS2DRenderer, CSS2DObject } from "three/examples/jsm/renderers/CSS2DRenderer";
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls";
import { ViewerEvents } from "viewers/common/viewer_events";

export class CanvasGraphics {
  constructor() {
    //set up camera
    const left = -this.CAMERA_HALF_WIDTH,
      right = this.CAMERA_HALF_WIDTH,
      top = this.CAMERA_HALF_HEIGHT,
      bottom = -this.CAMERA_HALF_HEIGHT,
      near = 0.001,
      far = 100;
    this.camera = new THREE.OrthographicCamera(
      left, right, top, bottom, near, far
    );
    this.resetCamera();
  }

  public initialiseCanvas(canvas: HTMLCanvasElement, canvasContainer: Element) {
    // initialise canvas
    this.canvas = canvas;
    this.canvasContainer = canvasContainer;
    this.enableOrbitControls();
  }

  public refreshCanvas() {
    if (!this.canvas) {
      return;
    }

    //set canvas size
    this.canvas.style.width = "100%";
    this.canvas.style.height = "40rem";

    this.orbit?.reset();

    // scene
    this.scene = new THREE.Scene();

    // renderers
    this.renderer = new THREE.WebGLRenderer({
      antialias: true,
      canvas: this.canvas,
      alpha: true
    });

    if (this.canvasContainer && this.canvasContainer.querySelector(".labels-canvas")) {
      this.labelRenderer = new CSS2DRenderer({
        element: this.canvasContainer.querySelector(".labels-canvas")! as HTMLElement
      });
    } else {
      this.labelRenderer = new CSS2DRenderer();
      this.labelRenderer.domElement.style.position = "absolute";
      this.labelRenderer.domElement.style.top = "0px";
      this.labelRenderer.domElement.style.width = "100%";
      this.labelRenderer.domElement.style.height = "40rem";
      this.labelRenderer.domElement.className = "labels-canvas";
      this.labelRenderer.domElement.style.pointerEvents = "none";
      this.canvasContainer?.appendChild(this.labelRenderer.domElement);
    }

    // set various factors for shading and shifting
    const visibleDarkFactor = 0, nonVisibleDarkFactor = 0, rectCounter = 0;
    const numberOfRects = this.rects.length;
    const numberOfVisibleRects = this.rects.filter(rect => rect.isVisible).length;
    const numberOfNonVisibleRects = this.rects.filter(rect => !rect.isVisible).length;

    const zShift = numberOfRects * this.layerSeparation;
    let xShift = 0, yShift = 3.5, labelYShift = 0;

    if (this.isLandscape) {
      xShift = 1;
      yShift = 1.5;
      labelYShift = 1.25;
    }

    const lowestY = Math.min(...this.rects.map(rect => {
      const y = rect.topLeft.y - rect.height + this.lowestYShift;
      if (this.isLandscape) {
        if (y < 0) {
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
      numberOfNonVisibleRects,
      nonVisibleDarkFactor,
      numberOfRects,
      xShift,
      yShift,
      zShift,
      lowestY
    );

    this.renderer.setSize(this.canvas!.clientWidth, this.canvas!.clientHeight);
    this.renderer.setPixelRatio(window.devicePixelRatio);
    this.renderer.compile(this.scene, this.camera);
    this.renderer.render(this.scene, this.camera);

    this.labelRenderer.setSize(this.canvas!.clientWidth, this.canvas!.clientHeight);
    this.labelRenderer.render(this.scene, this.camera);
  }

  public enableOrbitControls() {
    this.orbit = new OrbitControls(this.camera, this.canvas);
    this.orbit.enablePan = true;
    this.orbit.enableDamping = true;
    this.orbit.enableZoom = true;
    this.orbit.maxZoom = this.MAX_ZOOM;
    this.orbit.minZoom = this.MIN_ZOOM;
    this.orbit.panSpeed = this.PAN_SPEED;
    this.orbit.mouseButtons = { RIGHT: THREE.MOUSE.PAN };
    this.orbit.addEventListener("change", () => {
      this.fontSize = this.camera.zoom * this.INIT_FONT_SIZE;
      this.updateLabelsFontSize();
      if (this.scene && this.renderer && this.labelRenderer) {
        this.clearLabelElements();
        this.renderer.compile(this.scene, this.camera);
        this.renderer.render(this.scene, this.camera);
        this.labelRenderer.render(this.scene, this.camera);
        this.orbit?.saveState();
      }
    });
  }

  public getCamera() {
    return this.camera;
  }

  public getTargetObjects() {
    return this.targetObjects;
  }

  public getLayerSeparation() {
    return this.layerSeparation;
  }

  public getVisibleView() {
    return this.visibleView;
  }

  public getXCameraPos() {
    return this.camera.position.x;
  }

  public getShowVirtualDisplays() {
    return this.showVirtualDisplays;
  }

  public updateLayerSeparation(userInput: number) {
    this.layerSeparation = userInput;
  }

  public updateRotation(userInput: number) {
    this.camera.position.x = userInput;
    this.camera.position.y = Math.abs(userInput);
    this.labelShift = userInput / 4 * this.MAX_LABEL_SHIFT;
    this.lowestYShift = Math.abs(userInput) / 4 + 2;
    this.updateCameraAndControls();
  }

  public updateHighlightedItems(newItems: Array<string>) {
    this.highlightedItems = newItems;
  }

  public updateRects(rects: Rectangle[]) {
    this.rects = rects;
  }

  public updateIsLandscape(isLandscape: boolean) {
    this.isLandscape = isLandscape;
  }

  public updateVisibleView(visible: boolean) {
    this.visibleView = visible;
  }

  public updateVirtualDisplays(show: boolean) {
    this.showVirtualDisplays = show;
  }

  public resetCamera() {
    this.camera.lookAt(
      this.INIT_TARGET.x,
      this.INIT_TARGET.y,
      this.INIT_TARGET.z
    );
    this.camera.position.set(
      this.INIT_CAMERA_POS.x,
      this.INIT_CAMERA_POS.y,
      this.INIT_CAMERA_POS.z
    );
    this.camera.zoom = this.INIT_ZOOM;
    this.fontSize = this.INIT_FONT_SIZE;
    this.labelShift = this.MAX_LABEL_SHIFT;
    this.lowestYShift = this.INIT_LOWEST_Y_SHIFT;
    this.layerSeparation = this.INIT_LAYER_SEPARATION;
    this.camera.updateProjectionMatrix();
    if (this.canvas) {
      this.enableOrbitControls();
    }
    this.refreshCanvas();
  }

  public updateZoom(isZoomIn: boolean) {
    if (isZoomIn && this.camera.zoom < this.MAX_ZOOM) {
      this.camera.zoom += this.CAM_ZOOM_FACTOR;
      if (this.camera.zoom > this.MAX_ZOOM) this.camera.zoom = this.MAX_ZOOM;
    } else if (!isZoomIn && this.camera.zoom > this.MIN_ZOOM) {
      this.camera.zoom -= this.CAM_ZOOM_FACTOR;
      if (this.camera.zoom < this.MIN_ZOOM) this.camera.zoom = this.MIN_ZOOM;
    }
    this.fontSize = this.camera.zoom * this.INIT_FONT_SIZE;
    this.updateCameraAndControls();
  }

  private updateCameraAndControls() {
    this.camera.updateProjectionMatrix();
    this.orbit?.update();
    this.orbit?.saveState();
  }

  private drawScene(
    rectCounter: number,
    numberOfVisibleRects: number,
    visibleDarkFactor: number,
    numberOfNonVisibleRects: number,
    nonVisibleDarkFactor: number,
    numberOfRects: number,
    xShift: number,
    yShift: number,
    zShift: number,
    lowestY: number
  ) {
    this.targetObjects = [];
    this.clearLabelElements();
    this.rects.forEach(rect => {
      const mustNotDrawInVisibleView = this.visibleView && !rect.isVisible;
      const mustNotDrawInXrayViewWithoutVirtualDisplays =
        !this.visibleView && !this.showVirtualDisplays && rect.isVirtual;
      if (mustNotDrawInVisibleView || mustNotDrawInXrayViewWithoutVirtualDisplays) {
        rectCounter++;
        return;
      }

      //set colour mapping
      let planeColor;
      if (this.highlightedItems.includes(`${rect.id}`)) {
        planeColor = this.colorMapping("highlighted", numberOfRects, 0);
      } else if (rect.isVisible) {
        planeColor = this.colorMapping("green", numberOfVisibleRects, visibleDarkFactor);
        visibleDarkFactor++;
      } else {
        planeColor = this.colorMapping("grey", numberOfNonVisibleRects, nonVisibleDarkFactor);
        nonVisibleDarkFactor++;
      }

      //set plane geometry and material
      const geometry = new THREE.PlaneGeometry(rect.width, rect.height);
      const planeRect = this.setPlaneMaterial(rect, geometry, planeColor, xShift, yShift, zShift);
      this.scene?.add(planeRect);
      zShift -= this.layerSeparation;

      // bolder edges of each plane if in x-ray view
      if (!this.visibleView) {
        const edgeSegments = this.setEdgeMaterial(planeRect, geometry);
        this.scene?.add(edgeSegments);
      }

      // only some rects are clickable
      if (rect.isClickable) this.targetObjects.push(planeRect);

      // labelling elements
      if (rect.label.length > 0) {
        const circle = this.setCircleMaterial(planeRect, rect);
        this.scene?.add(circle);
        const [line, rectLabel] = this.createLabel(rect, circle, lowestY, rectCounter);
        this.scene?.add(line);
        this.scene?.add(rectLabel);
      }

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
    planeRect.position.y = rect.topLeft.y - rect.height / 2 + yShift;
    planeRect.position.x = rect.topLeft.x + rect.width / 2 - xShift;
    planeRect.position.z = zShift;
    planeRect.name = `${rect.id}`;
    return planeRect;
  }

  private setEdgeMaterial(planeRect: THREE.Mesh, geometry: THREE.PlaneGeometry) {
    const edgeColor = 0x000000;
    const edgeGeo = new THREE.EdgesGeometry(geometry);
    const edgeMaterial = new THREE.LineBasicMaterial({ color: edgeColor, linewidth: 1 });
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
      planeRect.position.x + rect.width / 2 - 0.05,
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
    let endPos;
    const labelYSeparation = 0.5;
    if (this.isLandscape) {
      endPos = new THREE.Vector3(
        circle.position.x, lowestY - 0.5 - rectCounter * labelYSeparation, circle.position.z
      );
    } else {
      endPos = new THREE.Vector3(
        circle.position.x, lowestY + 0.5 - rectCounter * labelYSeparation, circle.position.z
      );
    }

    const linePoints = [circle.position, endPos];


    //add rectangle label
    const rectLabelDiv: HTMLElement = document.createElement("div");
    rectLabelDiv.className = "rect-label";
    rectLabelDiv.textContent = labelText;
    rectLabelDiv.style.fontSize = `${this.fontSize}` + "px";
    rectLabelDiv.style.marginTop = "5px";
    if (isGrey) {
      rectLabelDiv.style.color = "grey";
    }
    rectLabelDiv.style.pointerEvents = "auto";
    rectLabelDiv.style.cursor = "pointer";
    rectLabelDiv.addEventListener(
      "click", (event) => this.propagateUpdateHighlightedItems(event, rect.id)
    );
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
        endPos.x + 0.6, endPos.y, endPos.z - 0.6
      );
    } else {
      rectLabel.position.set(
        endPos.x - labelWidth * this.LABEL_X_FACTOR,
        endPos.y - this.labelShift * labelWidth * this.LABEL_X_FACTOR,
        endPos.z
      );
    }

    const lineGeo = new THREE.BufferGeometry().setFromPoints(linePoints);
    const lineMaterial = new THREE.LineBasicMaterial({ color: isGrey ? 0x808080 : 0x000000 });
    const line = new THREE.Line(lineGeo, lineMaterial);

    return [line, rectLabel];
  }

  private propagateUpdateHighlightedItems(event: MouseEvent, newId: number) {
    event.preventDefault();
    const highlightedChangeEvent: CustomEvent = new CustomEvent(
      ViewerEvents.HighlightedChange,
      {
        bubbles: true,
        detail: { id: `${newId}` }
      });
    event.target?.dispatchEvent(highlightedChangeEvent);
  }

  private updateLabelsFontSize() {
    document.querySelectorAll(".rect-label").forEach(
      el => (el as HTMLElement).style.fontSize = `${this.fontSize}` + "px"
    );
  }

  private clearLabelElements() {
    document.querySelectorAll(".rect-label").forEach(el => el.remove());
  }

  private colorMapping(scale: string, numberOfRects: number, darkFactor: number): THREE.Color {
    if (scale === "highlighted") {
      return new THREE.Color(0xD2E3FC);
    } else if (scale === "grey") {
      // darkness of grey rect depends on z order - darkest 64, lightest 128
      //Separate RGB values between 0 and 1
      const lower = 120;
      const upper = 220;
      const darkness = ((upper - lower) * (numberOfRects - darkFactor) / numberOfRects + lower) / 255;
      return new THREE.Color(darkness, darkness, darkness);
    } else if (scale === "green") {
      // darkness of green rect depends on z order
      //Separate RGB values between 0 and 1
      const red = ((200 - 45) * (numberOfRects - darkFactor) / numberOfRects + 45) / 255;
      const green = ((232 - 182) * (numberOfRects - darkFactor) / numberOfRects + 182) / 255;
      const blue = ((183 - 44) * (numberOfRects - darkFactor) / numberOfRects + 44) / 255;
      return new THREE.Color(red, green, blue);
    } else {
      return new THREE.Color(0, 0, 0);
    }
  }

  private shortenText(text: string): string {
    if (text.length > 35) {
      text = text.slice(0, 35);
    }
    return text;
  }

  // dynamic scaling and canvas variables
  readonly CAMERA_HALF_WIDTH = 2.8;
  readonly CAMERA_HALF_HEIGHT = 3.2;
  private readonly MAX_LABEL_SHIFT = 0.305;
  private readonly MAX_ZOOM = 2.5;
  private readonly MIN_ZOOM = 0.5;
  private readonly INIT_ZOOM = 1;
  private readonly INIT_FONT_SIZE = 10;
  private readonly INIT_CAMERA_POS = new THREE.Vector3(4, 4, 6);
  private readonly INIT_TARGET = new THREE.Vector3(0, 0, 0);
  private readonly INIT_LAYER_SEPARATION = 0.4;
  private readonly INIT_LOWEST_Y_SHIFT = 3;
  private readonly PAN_SPEED = 1;
  private readonly LABEL_X_FACTOR = 0.009;
  private readonly CAM_ZOOM_FACTOR = 0.15;

  private fontSize = this.INIT_FONT_SIZE;
  private labelShift = this.MAX_LABEL_SHIFT;
  private lowestYShift = this.INIT_LOWEST_Y_SHIFT;
  private layerSeparation = this.INIT_LAYER_SEPARATION;

  private visibleView = false;
  private isLandscape = false;
  private showVirtualDisplays = false;
  private highlightedItems: Array<string> = [];
  private camera: THREE.OrthographicCamera;
  private scene?: THREE.Scene;
  private renderer?: THREE.WebGLRenderer;
  private labelRenderer?: CSS2DRenderer;
  private orbit?: OrbitControls;
  private rects: Rectangle[] = [];
  private targetObjects: any[] = [];
  private canvas?: HTMLCanvasElement;
  private canvasContainer?: Element;
}
