<script setup lang="ts">
import MainLayout from '../../components/MainLayout.vue';

const props = defineProps({
  class: {
    type: String,
  }
})
</script>

<template>
  <MainLayout>
    <div class="slidev-layout full w-full h-full" :class="props.class">
        <slot class="w-full h-full" />
    </div>
  </MainLayout>
</template>