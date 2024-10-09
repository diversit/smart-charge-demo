<script setup lang="ts">
import { computed } from 'vue'
import { handleBackground } from '../layoutHelper'
import MainLayout from '../../components/MainLayout.vue';

const props = defineProps({
  image: {
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
    <div class="slidev-layout w-full h-full" :style="style">
        <slot />
    </div>
  </MainLayout>
</template>