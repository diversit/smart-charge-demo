<script setup lang="ts">
import MainLayout from '../../components/MainLayout.vue';
</script>

<template>
  <MainLayout>
    <div class="slidev-layout section w-full h-full grid">
      <div class="my-auto text-center">
        <slot />
      </div>
    </div>
  </MainLayout>
</template>
