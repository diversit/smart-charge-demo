<script lang="ts">
export default {
    props: {
        swap: false,
    }
}
</script>

<template>
    <container class="flex flex-col h-full">
      <section class="flex-auto flex mx-0 px-0">
        <aside v-if="!swap" class="bg-white bg-[url(/images/devoxx-black.png)] dark:bg-black dark:bg-[url(/images/devoxx-white.png)] bg-center bg-no-repeat bg-center bg-contain bg-top w-22"/>
        <aside v-if="swap"  class="bg-black bg-[url(/images/devoxx-white.png)] bg-center bg-no-repeat bg-center bg-contain bg-top w-22"/>
        <slot/>
      </section>
      <footer class="flex-none flex bg-amber-500 text-2xl text-black font-sans">
        <div class="w-1/2 text-left pl-2">
          #Devoxx #SmartCharging
        </div>
        <div class="w-1/2 flex justify-end pr-2">
          
        </div>
      </footer>
    </container>
  </template>
  <!--
  <carbon:logo-github />
  <carbon:link />
  <carbon:logo-linkedin />
  <carbon:logo-twitter />
  -->