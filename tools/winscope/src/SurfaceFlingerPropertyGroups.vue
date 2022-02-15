<!-- Copyright (C) 2022 The Android Open Source Project

     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
-->
<template>
  <div>
    <div class="group">
      <span class="group-header">Geometry</span>
      <div class="left-column">
        <div class="column-header">Calculated</div>
        <span class="key">Transform:</span>
        <TransformMatrix :transform="layer.transform" />
        <div />
        <span class="key"
          >Crop:<md-tooltip
            >Raw value read from proto.bounds. This is the buffer size or
            requested crop cropped by parent bounds.</md-tooltip
          >
        </span>
        <span class="value">{{ layer.bounds }}</span>
        <div />
        <span class="key"
          >Final Bounds:<md-tooltip
            >Raw value read from proto.screenBounds. This is the calculated crop
            transformed.</md-tooltip
          ></span
        >
        <span class="value">{{ layer.screenBounds }}</span>
      </div>
      <div class="right-column">
        <div class="column-header">Requested</div>
        <span class="key">Transform:</span>
        <TransformMatrix :transform="layer.requestedTransform" />
        <div />
        <span class="key">Crop:</span>
        <span class="value">{{ layer.crop ? layer.crop : "[empty]" }}</span>
      </div>
    </div>
    <div class="group">
      <span class="group-header">
        <span class="group-heading">Buffer</span>
      </span>
      <div v-if="layer.isBufferLayer" class="left-column">
        <div />
        <span class="key">Size:</span>
        <span class="value">{{ layer.activeBuffer }}</span>
        <div />
        <span class="key">Frame Number:</span>
        <span class="value">{{ layer.currFrame }}</span>
        <div />
        <span class="key"
          >Transform:<md-tooltip
            >Rotates or flips the buffer in place. Used with display transform
            hint to cancel out any buffer transformation when sending to
            HWC.</md-tooltip
          ></span
        >
        <span class="value">{{ layer.bufferTransform }}</span>
      </div>
      <div v-if="layer.isBufferLayer" class="right-column">
        <div />
        <span class="key"
          >Destination Frame:<md-tooltip
            >Scales buffer to the frame by overriding the requested transform
            for this layer.</md-tooltip
          ></span
        >
        <span class="value">{{ layer.proto.destinationFrame }}</span>
        <div />
        <span
          v-if="(layer.flags & 0x400) /*eIgnoreDestinationFrame*/ === 0x400"
          class="value"
          >Destination Frame ignored because layer has eIgnoreDestinationFrame
          flag set.</span
        >
      </div>
      <div v-if="layer.isContainerLayer" class="left-column">
        <span class="key"></span> <span class="value">Container layer</span>
      </div>
      <div v-if="layer.isEffectLayer" class="left-column">
        <span class="key"></span> <span class="value">Effect layer</span>
      </div>
    </div>
    <div class="group">
      <span class="group-header">
        <span class="group-heading">Hierarchy</span>
      </span>
      <div class="left-column">
        <div />
        <span class="key">z-order:</span>
        <span class="value">{{ layer.z }}</span>
        <div />
        <span class="key"
          >relative parent:<md-tooltip
            >Layer is z-ordered relative to its relative parents but its bounds
            and other properties are inherited from its parents.</md-tooltip
          ></span
        >
        <span class="value">{{
          layer.zOrderRelativeOfId == -1 ? "none" : layer.zOrderRelativeOfId
        }}</span>
      </div>
    </div>
    <div class="group">
      <span class="group-header">
        <span class="group-heading">Effects</span>
      </span>
      <div class="left-column">
        <div class="column-header">Calculated</div>
        <span class="key">Color:</span>
        <span class="value">{{ layer.color }}</span>
        <div />
        <span class="key">Shadow:</span>
        <span class="value">{{ layer.shadowRadius }} px</span>
        <div />
        <span class="key">Corner Radius:</span>
        <span class="value"
          >radius:{{ formatFloat(layer.cornerRadius) }} px</span
        >
        <div />
        <span class="key"
          >Corner Radius Crop:<md-tooltip
            >Crop used to define the bounds of the corner radii. If the bounds
            are greater than the layer bounds then the rounded corner will not
            be visible.</md-tooltip
          ></span
        >
        <span class="value">{{ layer.cornerRadiusCrop }}</span>
        <div />
        <span class="key">Blur:</span>
        <span class="value"
          >{{
            layer.proto.backgroundBlurRadius
              ? layer.proto.backgroundBlurRadius
              : 0
          }}
          px</span
        >
      </div>
      <div class="right-column">
        <div class="column-header">Requested</div>
        <span class="key">Color:</span>
        <span class="value">{{ layer.requestedColor }}</span>
        <div />
        <span class="key">Shadow:</span>
        <span class="value"
          >{{
            layer.proto.requestedShadowRadius
              ? layer.proto.requestedShadowRadius
              : 0
          }}
          px</span
        >
        <div />
        <span class="key">Corner Radius:</span>
        <span class="value"
          >{{
            layer.proto.requestedCornerRadius
              ? formatFloat(layer.proto.requestedCornerRadius)
              : 0
          }}
          px</span
        >
      </div>
    </div>
    <div class="group">
      <span class="group-header">
        <span class="group-heading">Input</span>
      </span>
      <div v-if="layer.proto.inputWindowInfo" class="left-column">
        <span class="key">To Display Transform:</span>
        <TransformMatrix :transform="layer.inputTransform" />
        <div />
        <span class="key">Touchable Region:</span>
        <span class="value">{{ layer.inputRegion }}</span>
      </div>
      <div v-if="layer.proto.inputWindowInfo" class="right-column">
        <span class="key">Config:</span>
        <span class="value"></span>
        <div />
        <span class="key">Focusable:</span>
        <span class="value">{{ layer.proto.inputWindowInfo.focusable }}</span>
        <div />
        <span class="key">Crop touch region with layer:</span>
        <span class="value">{{
          layer.proto.inputWindowInfo.cropLayerId &lt;= 0
            ? "none"
            : layer.proto.inputWindowInfo.cropLayerId
        }}</span>
        <div />
        <span class="key">Replace touch region with crop:</span>
        <span class="value">{{
          layer.proto.inputWindowInfo.replaceTouchableRegionWithCrop
        }}</span>
      </div>
      <div v-if="!layer.proto.inputWindowInfo" class="left-column">
        <span class="key"></span>
        <span class="value">No input channel set</span>
      </div>
    </div>

    <div class="group">
      <span class="group-header">
        <span class="group-heading">Visibility</span>
      </span>
      <div class="left-column">
        <span class="key">Flags:</span>
        <span class="value">{{ layer.flags }}</span>
        <div />
        <div v-if="visibilityReason">
          <div v-for="reason in visibilityReason" v-bind:key="reason.key">
            <span class="key">{{ reason.key }}:</span>
            <span class="value">{{ reason.value }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import TransformMatrix from "@/TransformMatrix.vue";

export default {
  name: "SurfaceFlingerPropertyGroups",
  props: ["layer", "visibilityReason"],
  components: {
    TransformMatrix,
  },
  methods: {
    formatFloat(num) {
      return Math.round(num * 100) / 100;
    },
  },
};
</script>

<style scoped>
.group {
  padding: 0.5rem;
  border-bottom: thin solid rgba(0, 0, 0, 0.12);
  flex-direction: row;
  display: flex;
}

.group .key {
  font-weight: 500;
}

.group .value {
  color: rgba(0, 0, 0, 0.75);
}

.group-header {
  justify-content: center;
  padding: 0px 5px;
  width: 80px;
  display: inline-block;
  font-size: bigger;
  color: grey;
}

.left-column {
  width: 320px;
  max-width: 100%;
  display: inline-block;
  vertical-align: top;
  overflow: auto;
  border-right: 5px solid rgba(#000, 0.12);
  padding-right: 20px;
}
.right-column {
  width: 320px;
  max-width: 100%;
  display: inline-block;
  vertical-align: top;
  overflow: auto;
  border: 1px solid rgba(#000, 0.12);
}

.column-header {
  font-weight: lighter;
  font-size: smaller;
}

.element-summary {
  padding-top: 1rem;
}

.element-summary .key {
  font-weight: 500;
}

.element-summary .value {
  color: rgba(0, 0, 0, 0.75);
}
</style>
