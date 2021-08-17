import { createRouter, createWebHistory } from 'vue-router'
import JobList from '@/views/JobList.vue'
import JobDetails from '@/views/JobDetails.vue'
import About from '@/views/About.vue'
import JobConfigure from '@/views/JobConfigure.vue'
import NotFound from '@/views/NotFound.vue'

const routes = [
  {
    path: '/',
    name: 'JobList',
    component: JobList
  },
  {
    path: '/check-job/:id',
    name: 'JobDetails',
    props: true,
    component: JobDetails
  },
  {
    path: '/about',
    name: 'About',
    component: About
  },
  {
    path: '/create',
    name: 'Create',
    component: JobConfigure
  },
  {
    path: '/:catchAll(.*)',
    name: 'Not Found',
    component: NotFound
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

export default router
