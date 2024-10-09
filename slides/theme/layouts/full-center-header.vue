<!--
  Usage:
```md
---
layout: full-center-header
---
# Header on the left

::center::
# Center
Content in the center
```
-->
<script setup lang="ts">
import MainLayout from '../../components/MainLayout.vue';

const props = defineProps({
  class: {
    type: String,
  },
  layoutClass: {
    type: String,
  },
})
</script>

<template>
  <MainLayout>
    <div class="slidev-layout full w-full h-full">
        <div class="col-header">
          <slot class="w-full h-full" />
        </div>
        <div id="here" class="col-center center grid place-content-center" :class="props.class">
          <slot name="center"/>
        </div>
    </div>
  </MainLayout>
</template>