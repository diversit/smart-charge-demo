<script setup lang="ts">
import MainLayout from '../../components/MainLayout.vue';
import { computed } from 'vue'
import { handleBackground } from '../layoutHelper'

const props = defineProps({
  background: {
    // random image from a curated Unsplash collection by Anthony
    default: 'https://source.unsplash.com/collection/94734566/1920x1080',
  },
})

const style = computed(() => handleBackground(props.background, true))
</script>

<template>
  <MainLayout swap="true">
    <div class="slidev-layout cover text-center" :style="style">
      <div class="w-full">
        <slot />
      </div>
    </div>
  </MainLayout>
</template>
