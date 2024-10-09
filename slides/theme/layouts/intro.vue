<script setup lang="ts">
import MainLayout from '../../components/MainLayout.vue';
</script>

<template>
  <MainLayout>
    <div class="slidev-layout w-full cover text-center">
      <div class="my-auto">
        <slot />
      </div>
    </div>
  </MainLayout>
</template>