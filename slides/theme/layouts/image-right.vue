<script setup lang="ts">
import { computed } from 'vue'
import { handleBackground } from '../layoutHelper'
import MainLayout from '../../components/MainLayout.vue';

const props = defineProps({
  image: {
    type: String,
  },
  class: {
    type: String,
  },
  backgroundSize: {
    type: String,
    default: 'cover',
  },
})

const style = computed(() => handleBackground(props.image, false, props.backgroundSize))
</script>

<template>
  <MainLayout>
    <div class="grid grid-cols-2 w-full h-full auto-rows-fr">
        <div class="slidev-layout default" :class="props.class">
        <slot />
        </div>
        <div class="w-full h-full" :style="style" />
    </div>
  </MainLayout>
</template>