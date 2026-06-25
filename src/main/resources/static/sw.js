// Service Worker for VCSM PWA
const CACHE_NAME = 'vcsm-v1';
const OFFLINE_URL = '/offline';

// Assets to cache
const STATIC_ASSETS = [
  '/',
  '/css/style.css',
  '/css/ui.css',
  '/css/responsive.css',
  '/css/dark-mode.css',
  '/js/app.js',
  '/js/spinners.js',
  '/js/toast-notifications.js',
  '/js/dark-mode.js',
  '/js/back-to-top.js',
  '/offline'
];

// Install event - cache static assets
self.addEventListener('install', function(event) {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(function(cache) {
        console.log('Service Worker: Caching assets');
        return cache.addAll(STATIC_ASSETS);
      })
      .then(function() {
        return self.skipWaiting();
      })
  );
});

// Activate event - clean old caches
self.addEventListener('activate', function(event) {
  event.waitUntil(
    caches.keys().then(function(cacheNames) {
      return Promise.all(
        cacheNames.map(function(cacheName) {
          if (cacheName !== CACHE_NAME) {
            console.log('Service Worker: Deleting old cache', cacheName);
            return caches.delete(cacheName);
          }
        })
      );
    }).then(function() {
      return self.clients.claim();
    })
  );
});

// Fetch event - cache first strategy
self.addEventListener('fetch', function(event) {
  // Skip non-GET requests
  if (event.request.method !== 'GET') {
    event.respondWith(fetch(event.request));
    return;
  }

  // Skip API requests
  if (event.request.url.includes('/api/')) {
    event.respondWith(fetch(event.request));
    return;
  }

  // Skip admin pages
  if (event.request.url.includes('/admin/')) {
    event.respondWith(fetch(event.request));
    return;
  }

  // Skip external resources
  if (event.request.url.includes('googleapis') || 
      event.request.url.includes('cloudflare') || 
      event.request.url.includes('jsdelivr')) {
    event.respondWith(fetch(event.request));
    return;
  }

  // Cache first strategy
  event.respondWith(
    caches.match(event.request)
      .then(function(cachedResponse) {
        if (cachedResponse) {
          // Return cached response and update cache in background
          fetch(event.request)
            .then(function(response) {
              if (response && response.status === 200) {
                caches.open(CACHE_NAME)
                  .then(function(cache) {
                    cache.put(event.request, response.clone());
                  });
              }
            })
            .catch(function() {
              // Silent fail
            });
          return cachedResponse;
        }

        // If not in cache, fetch from network
        return fetch(event.request)
          .then(function(response) {
            // Cache the response for future
            if (response && response.status === 200) {
              caches.open(CACHE_NAME)
                .then(function(cache) {
                  cache.put(event.request, response.clone());
                });
            }
            return response;
          })
          .catch(function() {
            // If offline, show offline page
            return caches.match(OFFLINE_URL);
          });
      })
  );
});

// Push notification event
self.addEventListener('push', function(event) {
  let data = {};
  if (event.data) {
    try {
      data = event.data.json();
    } catch (e) {
      data = {
        title: 'VCSM Notification',
        body: event.data.text(),
        icon: '/icons/icon-192x192.png'
      };
    }
  }

  const options = {
    body: data.body || 'You have a new notification',
    icon: data.icon || '/icons/icon-192x192.png',
    badge: '/icons/icon-72x72.png',
    vibrate: [200, 100, 200],
    data: {
      url: data.url || '/'
    }
  };

  event.waitUntil(
    self.registration.showNotification(data.title || 'VCSM', options)
  );
});

// Notification click event
self.addEventListener('notificationclick', function(event) {
  event.notification.close();

  const url = event.notification.data?.url || '/';

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true })
      .then(function(clientList) {
        for (let i = 0; i < clientList.length; i++) {
          const client = clientList[i];
          if (client.url === url && 'focus' in client) {
            return client.focus();
          }
        }
        if (clients.openWindow) {
          return clients.openWindow(url);
        }
      })
  );
});