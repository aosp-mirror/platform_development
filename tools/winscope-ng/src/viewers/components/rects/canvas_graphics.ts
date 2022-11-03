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
    this.canvas.style.height = "100%";

    this.orbit?.reset();

    this.scene = new THREE.Scene();

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
      this.labelRenderer.domElement.style.height = "100%";
      this.labelRenderer.domElement.className = "labels-canvas";
      this.labelRenderer.domElement.style.pointerEvents = "none";
      this.canvasContainer?.appendChild(this.labelRenderer.domElement);
    }

    // set various factors for shading and shifting
    const numberOfRects = this.rects.length;

    const zShift = numberOfRects * this.layerSeparation;
    let xShift = 0, yShift = 3.5, labelYShift = 0;

    if (this.isLandscape) {
      xShift = -1;
      yShift = 2.5;
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
    xShift: number,
    yShift: number,
    zShift: number,
    lowestY: number
  ) {
    this.targetObjects = [];
    this.clearLabelElements();

    const darkFactors = this.computeRectDarkFactors(this.rects);

    for (const [rectIndex, rect] of this.rects.entries()) {
      const mustNotDrawInVisibleView = this.visibleView && !rect.isVisible;
      const mustNotDrawInXrayViewWithoutVirtualDisplays =
        !this.visibleView && !this.showVirtualDisplays && rect.isVirtual;
      if (mustNotDrawInVisibleView || mustNotDrawInXrayViewWithoutVirtualDisplays) {
        return;
      }

      const darkFactor = darkFactors[rectIndex];
      const rectGeometry = new THREE.PlaneGeometry(rect.width, rect.height);
      const rectMesh = this.makeRectMesh(rect, rectGeometry, xShift, yShift, zShift, darkFactor);
      this.scene?.add(rectMesh);

      // rect with bolder edges in x-ray view
      if (!this.visibleView) {
        const rectEdges = this.makeRectLineSegments(rectMesh, rectGeometry);
        this.scene?.add(rectEdges);
      }

      if (rect.isClickable) {
        this.targetObjects.push(rectMesh);
      }

      this.drawLabel(rect, rectMesh, lowestY, rectIndex);

      zShift -= this.layerSeparation;
    }
  }

  private computeRectDarkFactors(rects: Rectangle[]): number[] {
    let visibleRectsSoFar = 0;
    const visibleRectsTotal = rects.reduce((count, rect) => {
      return rect.isVisible ? count + 1 : count;
    }, 0);

    let nonVisibleRectsSoFar = 0;
    const nonVisibleRectsTotal = rects.reduce((count, rect) => {
      return rect.isVisible ? count : count + 1;
    }, 0);

    const factors = rects.map(rect => {
      if (rect.isVisible) {
        return (visibleRectsTotal - visibleRectsSoFar++) / visibleRectsTotal;
      } else {
        return (nonVisibleRectsTotal - nonVisibleRectsSoFar++) / nonVisibleRectsTotal;
      }
    });

    return factors;
  }

  private drawLabel(rect: Rectangle, rectMesh: THREE.Mesh, lowestY: number, rectCounter: number) {
    if (rect.label.length == 0) {
      return;
    }

    const circleMesh = this.makeLabelCircleMesh(rectMesh, rect);
    this.scene?.add(circleMesh);

    const isGray = !this.visibleView && !rect.isVisible;

    let lineEndPos;
    const labelYSeparation = 0.5;
    if (this.isLandscape) {
      lineEndPos = new THREE.Vector3(
        circleMesh.position.x, lowestY - 0.5 - rectCounter * labelYSeparation, circleMesh.position.z
      );
    } else {
      lineEndPos = new THREE.Vector3(
        circleMesh.position.x, lowestY + 0.5 - rectCounter * labelYSeparation, circleMesh.position.z
      );
    }

    const linePoints = [circleMesh.position, lineEndPos];
    const lineGeo = new THREE.BufferGeometry().setFromPoints(linePoints);
    const lineMaterial = new THREE.LineBasicMaterial({ color: isGray ? 0x808080 : 0x000000 });
    const line = new THREE.Line(lineGeo, lineMaterial);
    this.scene?.add(line);

    this.drawLabelTextHtml(lineEndPos, rect, isGray);
  }

  private drawLabelTextHtml(position: THREE.Vector3, rect: Rectangle, isGray: boolean) {
    // Add rectangle label
    const spanText: HTMLElement = document.createElement("span");
    spanText.innerText = this.shortenText(rect.label);
    spanText.className = "mat-body-1";

    // Hack: transparent/placeholder text used to push the visible text towards left
    // (towards negative x) and properly align it with the label's vertical segment
    const spanPlaceholder: HTMLElement = document.createElement("span");
    spanPlaceholder.innerText = this.shortenText(rect.label);
    spanPlaceholder.className = "mat-body-1";
    spanPlaceholder.style.opacity = "0";

    const div: HTMLElement = document.createElement("div");
    div.className = "rect-label";
    div.style.display = "inline";
    div.appendChild(spanText);
    div.appendChild(spanPlaceholder);

    div.style.marginTop = "5px";
    if (isGray) {
      div.style.color = "gray";
    }
    div.style.pointerEvents = "auto";
    div.style.cursor = "pointer";
    div.addEventListener(
      "click", (event) => this.propagateUpdateHighlightedItems(event, rect.id)
    );

    const label = new CSS2DObject(div);
    label.name = rect.label;
    label.position.set(
      position.x,
      position.y,
      position.z
    );

    this.scene?.add(label);
  }

  private makeRectMesh(
    rect: Rectangle,
    geometry: THREE.PlaneGeometry,
    xShift: number,
    yShift: number,
    zShift: number,
    darkFactor: number,
  ): THREE.Mesh {
    let color: THREE.Color;

    if (this.highlightedItems.includes(`${rect.id}`)) {
      color = new THREE.Color(0xD2E3FC);
    } else if (rect.isVisible) {
      // green (darkness depends on z order)
      const red = ((200 - 45) * darkFactor + 45) / 255;
      const green = ((232 - 182) * darkFactor + 182) / 255;
      const blue = ((183 - 44) * darkFactor + 44) / 255;
      color = new THREE.Color(red, green, blue);
    } else {
      // gray (darkness depends on z order)
      const lower = 120;
      const upper = 220;
      const darkness = ((upper - lower) * darkFactor + lower) / 255;
      color = new THREE.Color(darkness, darkness, darkness);
    }

    const mesh = new THREE.Mesh(
      geometry,
      new THREE.MeshBasicMaterial({
        color: color,
        opacity: this.visibleView ? 1 : 0.75,
        transparent: true,
      }));
    mesh.position.y = rect.topLeft.y - rect.height / 2 + yShift;
    mesh.position.x = rect.topLeft.x + rect.width / 2 - xShift;
    mesh.position.z = zShift;
    mesh.name = `${rect.id}`;

    return mesh;
  }

  private makeRectLineSegments(rectMesh: THREE.Mesh, geometry: THREE.PlaneGeometry): THREE.LineSegments {
    const edgeColor = 0x000000;
    const edgeGeo = new THREE.EdgesGeometry(geometry);
    const edgeMaterial = new THREE.LineBasicMaterial({ color: edgeColor, linewidth: 1 });
    const edgeSegments = new THREE.LineSegments(
      edgeGeo, edgeMaterial
    );
    edgeSegments.position.set(rectMesh.position.x, rectMesh.position.y, rectMesh.position.z);
    return edgeSegments;
  }

  private makeLabelCircleMesh(rectMesh: THREE.Mesh, rect: Rectangle): THREE.Mesh {
    const geometry = new THREE.CircleGeometry(0.02, 200);
    const material = new THREE.MeshBasicMaterial({ color: 0x000000 });
    const mesh = new THREE.Mesh(geometry, material);
    mesh.position.set(
      rectMesh.position.x + rect.width / 2 - 0.05,
      rectMesh.position.y,
      rectMesh.position.z + 0.05
    );
    mesh.rotateY(THREE.MathUtils.degToRad(30));
    return mesh;
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

  private shortenText(text: string): string {
    if (text.length > 35) {
      text = text.slice(0, 35);
    }
    return text;
  }

  // dynamic scaling and canvas variables
  readonly CAMERA_HALF_WIDTH = 2.8;
  readonly CAMERA_HALF_HEIGHT = 3.2;
  private readonly MAX_ZOOM = 2.5;
  private readonly MIN_ZOOM = 0.5;
  private readonly INIT_ZOOM = 0.75;
  private readonly INIT_FONT_SIZE = 10;
  private readonly INIT_CAMERA_POS = new THREE.Vector3(4, 4, 6);
  private readonly INIT_TARGET = new THREE.Vector3(0, 0, 0);
  private readonly INIT_LAYER_SEPARATION = 0.4;
  private readonly INIT_LOWEST_Y_SHIFT = 3;
  private readonly PAN_SPEED = 1;
  private readonly CAM_ZOOM_FACTOR = 0.15;

  private fontSize = this.INIT_FONT_SIZE;
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
