<template>
  <md-menu-item :class="optionClasses" :disabled="isDisabled" @click="setSelection">
    <md-checkbox class="md-primary" v-model="isChecked" v-if="MdSelect.multiple" :disabled="isDisabled" />

    <div class="item">
      <i class="material-icons" v-if="icon">
        {{ icon }}
      </i>
      <span class="value">
        {{ displayValue || value }}
      </span>
      <span class="material-icons help-icon">
        help_outline
        <md-tooltip md-direction="right">{{ desc }}</md-tooltip>
      </span>
    </div>
  </md-menu-item>
</template>

<script>
  export default {
    name: 'MdIconOption',
    props: {
      // Serves as key for option (should be unique within an MdSelect)
      // Also serves as the backup to displayValue if null
      value: [String, Number, Boolean],
      // Value shown to describe an option in dropdown selection
      displayValue: [String, Number, Boolean],
      // If present, this is shown to represent item when dropdown is collapsed
      shortValue: [String, Number, Boolean],
      icon: String,
      desc: String,
      disabled: Boolean
    },
    inject: {
      MdSelect: {},
      MdOptgroup: {
        default: {}
      }
    },
    data: () => ({
      isSelected: false,
      isChecked: false
    }),
    computed: {
      selectValue () {
        return this.MdSelect.modelValue
      },
      isMultiple () {
        return this.MdSelect.multiple
      },
      isDisabled () {
        return this.MdOptgroup.disabled || this.disabled
      },
      key () {
        return this.value;
      },
      inputLabel () {
        return this.MdSelect.label
      },
      optionClasses () {
        return {
          'md-selected': this.isSelected || this.isChecked
        }
      }
    },
    watch: {
      selectValue () {
        this.setIsSelected()
      },
      isChecked (val) {
        if (val === this.isSelected) {
          return
        }
        this.setSelection()
      },
      isSelected (val) {
        this.isChecked = val
      }
    },
    methods: {
      getTextContent () {
        return this.shortValue || this.displayValue || this.value;
      },
      setIsSelected () {
        if (!this.isMultiple) {
          this.isSelected = this.selectValue === this.value
          return
        }
        if (this.selectValue === undefined) {
          this.isSelected = false
          return
        }
        this.isSelected = this.selectValue.includes(this.value)
      },
      setSingleSelection () {
        this.MdSelect.setValue(this.value)
      },
      setMultipleSelection () {
        this.MdSelect.setMultipleValue(this.value)
      },
      setSelection () {
        if (!this.isDisabled) {
          if (this.isMultiple) {
            this.setMultipleSelection()
          } else {
            this.setSingleSelection()
          }
        }
      },
      setItem () {
        this.$set(this.MdSelect.items, this.key, this.getTextContent())
      }
    },
    updated () {
      this.setItem()
    },
    created () {
      this.setItem()
      this.setIsSelected()
    }
  }
</script>

<style scoped>
.item {
  display: inline-flex;
  align-items: center;
  width: 100%;
  flex: 1;
}

.item .value {
  flex-grow: 1;
  margin: 0 10px;
}

.item .help-icon {
  font-size: 15px;
}
</style>