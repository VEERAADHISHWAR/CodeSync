import { Client } from '@stomp/stompjs'

const client = new Client({

    brokerURL: 'ws://localhost:8080/ws',

    reconnectDelay: 5000,

    onConnect: () => {
        console.log('WebSocket connected successfully')
    },

    onDisconnect: () => {
        console.log('WebSocket disconnected')
    },

    onStompError: (frame) => {
        console.error('WebSocket error:', frame)
    }

})

export default client