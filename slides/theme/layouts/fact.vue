<script setup lang="ts">
import MainLayout from '../../components/MainLayout.vue';
</script>

<template>
  <MainLayout>
    <div class="slidev-layout fact">
      <div class="my-auto">
        <slot />
      </div>
    </div>
  </MainLayout>
</template>
