import { createRouter, createWebHistory } from 'vue-router'
import JobList from '@/views/JobList.vue'
import JobDetails from '@/views/JobDetails.vue'
import About from '@/views/About.vue'
import JobConfigure from '@/views/JobConfigure.vue'

const routes = [
  {
    path: '/',
    name: 'JobList',
    component: JobList
  },
  {
    path: '/check/:id',
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
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

export default router
