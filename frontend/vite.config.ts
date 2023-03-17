import {fileURLToPath, URL} from 'node:url'

import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import path from "path";

export default defineConfig(({}) => {
    const plugins = [
        vue(),
        vueJsx(),
    ];

    return {
        plugins: plugins,
        resolve: {
            alias: {
                '@': fileURLToPath(new URL('./src', import.meta.url)),
                '~bootstrap': path.resolve(__dirname, 'node_modules/bootstrap'),
                '~bootstrap-icons': path.resolve(__dirname, 'node_modules/bootstrap-icons'),
            }
        },
        build: {
            sourcemap: true,
        },
        base: './',
        server: {
            proxy: {
                '/api': {
                    target: 'http://localhost:8080'
                }
            }
        }

    }
})
