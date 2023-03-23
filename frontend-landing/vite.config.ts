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
            port: 5374,
            proxy: {
                '/service-participants': {
                    target: 'http://localhost:8081',
                    rewrite: (orig) => orig.replace("/service-participants", ""),
                },
                '/service-auction-object-registry': {
                    target: 'http://localhost:8082',
                    rewrite: (orig) => orig.replace("/service-auction-object-registry", ""),
                },
                '/service-auction-query': {
                    target: 'http://localhost:8083',
                    rewrite: (orig) => orig.replace("/service-auction-query", ""),
                },
                '/service-auctions': {
                    target: 'http://localhost:8084',
                    rewrite: (orig) => orig.replace("/service-auctions", ""),
                },
                '/service-interfaces': {
                    target: 'http://localhost:8087',
                    rewrite: (orig) => orig.replace("/service-interfaces", ""),
                },
                '/axon-server': {
                    target: 'http://localhost:9024',
                    rewrite: (orig) => orig.replace("/axon-server", ""),
                },
                '/jaeger': {
                    target: 'http://localhost:16686',

                },
                '/grafana': {
                    target: 'http://localhost:3000',
                },
                '/prometheus': {
                    target: 'http://localhost:9090',
                    rewrite: (orig) => orig.replace("/prometheus", ""),
                },
            }
        }

    }
})
