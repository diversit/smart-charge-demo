<script setup lang="ts">
import MainLayout from '../../components/MainLayout.vue';
</script>

<template>
  <MainLayout swap="true">
    <div class="slidev-layout end w-full">
        <slot>END</slot>
    </div>
  </MainLayout>
</template>

<style scoped lang="postcss">
.slidev-layout.end {
  @apply text-white text-opacity-85 text-xl tracking-widest bg-black h-full text-center grid place-content-center select-none;
}
</style>