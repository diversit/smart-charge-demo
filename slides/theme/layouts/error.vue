<script setup lang="ts">
import MainLayout from '../../components/MainLayout.vue';
</script>

<template>
  <MainLayout>
    <div class="px-4 py-10 text-center text-red-700 dark:text-red-500 font-bold font-mono">
        <slot/>
    </div>
  </MainLayout>
</template>