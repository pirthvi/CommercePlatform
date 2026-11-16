const { before, after, test } = require('node:test');
const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const { once } = require('node:events');
const { readdirSync, rmSync } = require('node:fs');
const path = require('node:path');
const { randomUUID } = require('node:crypto');
const { setTimeout: delay } = require('node:timers/promises');

const backend = path.resolve(__dirname, '..');
const adminPassword = randomUUID();
const testDatabase = 'commerce-test-' + randomUUID();
const png = new Blob([Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/5nUAAAAASUVORK5CYII=',
  'base64',
)], { type: 'image/png' });
let api;
let server;
let token;

async function request(method, route, { body, bearer, status = 200, headers = {} } = {}) {
  const multipart = body instanceof FormData;
  const response = await fetch(api + route, {
    method,
    headers: {
      ...(bearer ? { Authorization: 'Bearer ' + bearer } : {}),
      ...(body && !multipart ? { 'Content-Type': 'application/json' } : {}),
      ...headers,
    },
    body: multipart ? body : JSON.stringify(body),
  });
  const result = await response.text();
  assert.equal(response.status, status, method + ' ' + route + ' ' + result);
  return result && JSON.parse(result);
}

function multipart(key, value) {
  const form = new FormData();
  form.append(key, new Blob([JSON.stringify(value)], { type: 'application/json' }));
  return form;
}

async function startServer() {
  const target = path.join(backend, 'target');
  const jar = readdirSync(target).find(name => /^commerce-api-[\d.]+(?:-SNAPSHOT)?\.jar$/.test(name));
  assert.ok(jar, 'Build the backend with mvn package first.');
  const java = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', 'java') : 'java';
  server = spawn(java, [
    '-jar', path.join(target, jar), '--server.address=127.0.0.1', '--server.port=0',
    '--spring.datasource.driver-class-name=org.h2.Driver', '--spring.jpa.database-platform=org.hibernate.dialect.H2Dialect',
    '--spring.datasource.url=jdbc:h2:file:./' + testDatabase + ';WRITE_DELAY=0', '--spring.datasource.username=sa', '--spring.datasource.password=',
    '--spring.jpa.hibernate.ddl-auto=update', '--app.admin.username=test-admin', '--app.admin.password=' + adminPassword,
  ], { cwd: target, windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'] });
  let log = '';
  let spawnError;
  server.on('error', error => { spawnError = error; });
  server.stdout.on('data', data => { log += data; });
  server.stderr.on('data', data => { log += data; });
  for (let attempt = 0; attempt < 240; attempt++) {
    if (spawnError) throw spawnError;
    if (server.exitCode !== null) throw new Error(log.slice(-2500));
    const port = /Tomcat started on port (\d+)/.exec(log)?.[1];
    if (port) {
      api = 'http://127.0.0.1:' + port + '/api';
      const ready = await fetch(api + '/content/layouts/template/home');
      const layout = ready.ok ? await ready.json() : null;
      if (layout?.sections.some(section => section.type === 'product-grid' && section.productIds?.length)) {
        const login = await request('POST', '/admin/session', { body: { username: 'test-admin', password: adminPassword } });
        token = login.accessToken;
        return;
      }
      if (!ready.ok) await ready.arrayBuffer();
    }
    await delay(250);
  }
  throw new Error('Test API did not become ready.');
}

async function stopServer() {
  if (server?.pid && server.exitCode === null && !server.killed) {
    const stopped = once(server, 'exit');
    server.kill();
    await stopped;
  }
}

before(startServer);
after(async () => {
  await stopServer();
  for (const extension of ['.mv.db', '.trace.db', '.lock.db']) {
    rmSync(path.join(backend, 'target', testDatabase + extension), { force: true });
  }
});

test('a new apparel store has photographed pages and stocked products that can be ordered', async () => {
  const site = await request('GET', '/content/site');
  const products = await request('GET', '/products');
  const categories = await request('GET', '/categories');
  assert.equal(site.brandName, 'Everyday Apparel');
  assert.equal(products.length, 4);
  assert.deepEqual(new Set(products.map(p => p.category)), new Set(['Tops', 'Trousers']));
  for (const link of site.navigation.filter(item => item.category)) {
    assert.ok(categories.some(c => c.slug === link.category));
  }
  const photos = new Set();
  for (const product of products) {
    assert.ok(product.stock > 0 && product.price > 0 && product.weight && product.ingredients);
    assert.equal((await request('GET', '/products/slug/' + product.slug)).id, product.id);
    const ledger = await request('GET', '/admin/inventory/' + product.id + '/movements', { bearer: token });
    assert.equal(ledger[0].balance, product.stock);
    photos.add(product.imageUrl);
    photos.add(product.galleryImages);
  }
  for (const key of ['home','shop','product','cart','checkout','login','register','account','order-success','about','shipping','contact']) {
    const kind = ['about','shipping','contact'].includes(key) ? 'page' : 'template';
    const page = await request('GET', '/content/layouts/' + kind + '/' + key);
    assert.ok(page.sections.length >= 2 && page.published, key);
    const image = page.sections.find(section => section.imageUrl && section.imageAlt);
    assert.ok(image, 'Page photo for ' + key);
    photos.add(image.imageUrl);
    if (key === 'home') {
      const edit = page.sections.find(s => s.type === 'product-grid');
      assert.deepEqual(new Set(edit.productIds), new Set(products.map(p => p.id)));
    }
    if (key === 'shop' || key === 'product') {
      const block = page.sections.find(s => s.type === (key === 'shop' ? 'catalog' : 'related-products'));
      assert.deepEqual(block.items.map(item => item.category), ['tops', 'trousers']);
    }
  }
  for (const path of photos) {
    assert.match(path, /^\/api\//);
    const response = await fetch(new URL(path, api));
    assert.equal(response.status, 200);
    assert.equal(response.headers.get('content-type'), 'image/jpeg');
    const bytes = Buffer.from(await response.arrayBuffer());
    assert.deepEqual([...bytes.subarray(0, 3)], [255, 216, 255]);
    assert.ok(bytes.length > 4096);
  }
  const product = products[0];
  const headers = { 'X-Guest-Cart-Id': randomUUID() };
  await request('POST', '/cart/items', { headers, body: { productId: product.id, quantity: 1 } });
  const order = await request('POST', '/orders', { headers, status: 201, body: {
    customerName: 'Apparel Buyer', email: 'apparel@example.test', phone: '12345', address: 'Test street',
    city: 'City', postalCode: '10000', items: [{ productId: product.id, quantity: 1 }],
  } });
  assert.equal(order.items[0].productName, product.name);
  assert.equal((await request('GET', '/products/slug/' + product.slug)).stock, product.stock - 1);
});

test('category identity, customer cart merge and stock changes survive the full order flow', async () => {
  const category = await request('POST', '/admin/categories', { bearer: token, status: 201, body: { name: 'Stationery' } });
  const draft = { name: 'Notebook', slug: 'notebook', description: 'Ruled paper', categoryId: category.id,
    price: 12.50, stock: 5, active: true };
  const form = multipart('product', draft);
  form.append('mainImage', png, 'notebook.png');
  const product = await request('POST', '/admin/products', { bearer: token, status: 201, body: form });
  await request('PUT', '/admin/categories/' + category.id, { bearer: token, body: { name: 'Office' } });
  const [listed] = await request('GET', '/products?categoryId=' + category.id);
  assert.deepEqual([listed.id, listed.categoryId, listed.category, listed.categorySlug], [product.id, category.id, 'Office', category.slug]);
  await request('DELETE', '/admin/categories/' + category.id, { bearer: token, status: 409 });

  const stock = '/admin/inventory/' + product.id;
  await request('POST', stock + '/adjustments', { bearer: token, body: { change: 2, expectedStock: 5, reason: 'Delivery' } });
  await request('POST', stock + '/adjustments', { bearer: token, status: 409, body: { change: 2, expectedStock: 5, reason: 'Stale count' } });
  await request('PUT', '/admin/products/' + product.id, { bearer: token, body: multipart('product', { ...draft, previousStock: 5 }) });
  const headers = { 'X-Guest-Cart-Id': randomUUID() };
  await request('POST', '/cart/items', { headers, body: { productId: product.id, quantity: 2 } });
  const account = await request('POST', '/auth/register', {
    status: 201, body: { fullName: 'Test Buyer', email: 'buyer@example.test', password: randomUUID() },
  });
  const cart = await request('POST', '/cart/merge', { headers, bearer: account.accessToken, body: {} });
  assert.equal(cart.items[0].product.categoryId, category.id);
  await request('PUT', '/admin/commerce', { bearer: token, body: { currency: 'USD', deliveryFee: 3, freeDeliveryFrom: 50 } });
  const order = await request('POST', '/orders', {
    bearer: account.accessToken, headers, status: 201,
    body: { customerName: 'Test Buyer', email: 'buyer@example.test', phone: '12345', address: 'Test street',
      city: 'City', postalCode: '10000', items: [{ productId: product.id, quantity: 2 }] },
  });
  assert.deepEqual([order.currency, order.total], ['USD', 28]);
  const [adminOrder] = await request('GET', '/admin/orders', { bearer: token });
  const setStatus = status => request('PATCH', '/admin/orders/' + adminOrder.id + '/status', { bearer: token, body: { status } });
  await Promise.all([setStatus('CANCELLED'), setStatus('CANCELLED')]);
  await setStatus('PROCESSING');
  const ledger = await request('GET', stock + '/movements', { bearer: token });
  assert.deepEqual(ledger.map(row => row.balance), [5, 7, 5, 7, 5]);
  assert.deepEqual((await request('GET', '/cart', { bearer: account.accessToken, headers })).items, []);
});

test('a product page publishes shared text and icon bytes together', async () => {
  const change = { texts: { addToCart: 'Add to basket' }, symbols: { delivery: '✦' }, removeImages: [] };
  const form = multipart('changes', change);
  form.append('icon_payment', png, 'payment.png');
  await request('PUT', '/admin/product-page', { body: form, status: 401 });
  await request('PUT', '/admin/product-page', { body: form, bearer: token });
  const saved = await request('GET', '/content/product-page');
  assert.equal(saved.texts.addToCart, 'Add to basket');
  assert.equal(saved.symbols.delivery, '✦');
  const image = await fetch(new URL(saved.images.payment, api));
  assert.equal(image.headers.get('content-type'), 'image/png');
  assert.deepEqual(Buffer.from(await image.arrayBuffer()), Buffer.from(await png.arrayBuffer()));

  const invalid = multipart('changes', { ...change, texts: { addToCart: 'Incomplete change' } });
  invalid.append('icon_payment', new Blob(['invalid']), 'invalid.png');
  await request('PUT', '/admin/product-page', { bearer: token, body: invalid, status: 400 });
  assert.equal((await request('GET', '/content/product-page')).texts.addToCart, 'Add to basket');
  await request('PUT', '/admin/product-page', { bearer: token, body: multipart('changes', { texts: {}, symbols: {}, removeImages: ['payment'] }) });
  assert.equal((await request('GET', '/content/product-page')).images.payment, undefined);
});

test('page layouts publish ordered blocks and uploaded images atomically', async () => {
  const layout = {title: 'Field guide', slug: 'field-guide', metaDescription: 'Seasonal notes', published: false, revision: 0,
    sections: [
      { id: 'a', type: 'gallery', visible: true, items: [{id: 'photo', title: 'Summer', imageAlt: 'Summer landscape'}] },
      { id: 'b', type: 'faq', visible: false, heading: 'Questions', items: [{id:'q',title:'When?',body:'In July.'}] },
    ] };
  const form = multipart('layout', layout);
  form.append('image_photo', png, 'summer.png');
  await request('POST','/admin/page-layouts',{body:form,status:401});
  const draft = await request('POST','/admin/page-layouts',{bearer:token,body:form,status:201});
  assert.equal(draft.revision, 1);
  await request('GET','/content/layouts/page/field-guide',{status:404});
  const bytes = draft.sections[0].items[0].imageUrl;
  assert.deepEqual(Buffer.from(await (await fetch(new URL(bytes,api))).arrayBuffer()),Buffer.from(await png.arrayBuffer()));
  const edit = { ...draft, published:true, sections:[draft.sections[1], draft.sections[0]] };
  const url = '/admin/page-layouts/' + draft.id;
  const valid = await request('PUT',url,{bearer:token,body:multipart('layout',edit)});
  const live = await request('GET','/content/layouts/page/field-guide');
  assert.deepEqual(live.sections.map(b=>b.id),['a']);
  assert.deepEqual(valid.sections.map(b=>b.id),['b','a']);
  await request('PUT',url,{bearer:token,body:multipart('layout',edit),status:409});
  const invalid = multipart('layout',{...valid, title:'Uncommitted change'});
  invalid.append('image_photo', new Blob(['invalid']), 'invalid.png');
  await request('PUT',url,{bearer:token,body:invalid,status:400});
  assert.equal((await request('GET','/content/layouts/page/field-guide')).title,'Field guide');
  const removed = await request('PUT',url,{bearer:token,body:multipart('layout',{...valid,sections:[valid.sections[0]]})});
  assert.equal(removed.revision, valid.revision + 1);
  assert.equal((await fetch(new URL(bytes,api))).status,404);
  await request('DELETE','/admin/pages/'+draft.id,{bearer:token,status:204});
  await request('GET','/content/layouts/page/field-guide',{status:404});
});

test('system layouts keep their commerce controls and the legacy editor still updates custom pages', async () => {
  for (const key of ['home','shop','product','cart','checkout','login','register','account','order-success']) {
    const page = await request('GET','/content/layouts/template/'+key);
    assert.ok(page.sections.length, key);
    for (const kind of page.required) assert.equal(page.sections.filter(s=>s.type===kind).length,1);
  }
  const cart = await request('GET','/admin/page-layouts/template/cart',{bearer:token});
  await request('PUT','/admin/page-layouts/'+cart.id,{bearer:token,body:multipart('layout',{...cart,sections:[]}),status:400});
  await request('DELETE','/admin/pages/'+cart.id,{bearer:token,status:409});
  const legacy = {title:'Old page',slug:'legacy',eyebrow:'Notes',body:'First paragraph',metaDescription:'Summary',buttonLabel:'Shop',buttonLink:'/shop',published:true};
  const p = await request('POST','/admin/pages',{bearer:token,body:multipart('page',legacy),status:201});
  await request('PUT','/admin/pages/'+p.id,{bearer:token,body:multipart('page',{...legacy,body:'Edited in the previous editor'})});
  const layout = await request('GET','/content/layouts/page/legacy');
  assert.deepEqual(layout.sections.map(b=>b.type),['text','button']);
  assert.equal(layout.sections[0].body,'Edited in the previous editor');
});

test('mapped category selections must exist and stay valid while used on a page', async () => {
  const category = await request('POST','/admin/categories',{bearer:token,status:201,body:{name:'Layout category'}});
  const page={title:'Shelf',slug:'layout-shelf',metaDescription:'',published:true,revision:0,
    sections:[{id:'shelf',type:'product-grid',visible:true,categoryId:category.id,items:[],productIds:[]}]};
  const saved=await request('POST','/admin/page-layouts',{bearer:token,status:201,body:multipart('layout',page)});
  await request('DELETE','/admin/categories/'+category.id,{bearer:token,status:409});
  const missing={...saved,sections:[{...saved.sections[0],productIds:[999999999]}]};
  await request('PUT','/admin/page-layouts/'+saved.id,{bearer:token,status:400,body:multipart('layout',missing)});
  await request('DELETE','/admin/pages/'+saved.id,{bearer:token,status:204});
  await request('DELETE','/admin/categories/'+category.id,{bearer:token,status:204});
});

test('publishing a new Home component preserves the header and protects the layout from older editors', async () => {
  const site = await request('GET', '/content/site');
  const home = await request('GET', '/admin/page-layouts/template/home', { bearer: token });
  const faq = { id: 'home-faq', type: 'faq', heading: 'Delivery questions', visible: true,
    items: [{ id: 'collection', title: 'Where can I collect?', body: 'At our shop.' }] };
  await request('PUT', '/admin/page-layouts/' + home.id, {
    bearer: token, body: multipart('layout', { ...home, sections: [...home.sections, faq] }),
  });
  const updated = await request('GET', '/content/site');
  assert.equal(updated.brandName, site.brandName);
  assert.deepEqual(updated.navigation, site.navigation);
  assert.deepEqual(updated.footerColumns, site.footerColumns);
  await request('PUT', '/admin/site-content/home', {
    bearer: token, status: 409, body: multipart('content', { homeSections: site.homeSections }),
  });
  const live = await request('GET', '/content/layouts/template/home');
  assert.deepEqual(live.sections.at(-1), faq);
});

test('restarting preserves the edited cart page, a stock adjustment and a removed sample style', async () => {
  const cart = await request('GET', '/admin/page-layouts/template/cart', { bearer: token });
  const sections = [{ ...cart.sections[0], heading: 'My shopping cart', imageUrl: '' }];
  const saved = await request('PUT', '/admin/page-layouts/' + cart.id, {
    bearer: token, body: multipart('layout', { ...cart, sections }),
  });
  const shirt = await request('GET', '/products/slug/sage-oxford-shirt');
  await request('POST', '/admin/inventory/' + shirt.id + '/adjustments', {
    bearer: token, body: { change: -2, expectedStock: shirt.stock, reason: 'Stock count' },
  });
  const removed = await request('GET', '/products/slug/sand-linen-trousers');
  await request('DELETE', '/admin/products/' + removed.id, { bearer: token, status: 204 });
  await stopServer();
  await startServer();
  assert.deepEqual(await request('GET', '/admin/page-layouts/template/cart', { bearer: token }), saved);
  assert.equal((await request('GET', '/products/slug/sage-oxford-shirt')).stock, shirt.stock - 2);
  await request('GET', '/products/slug/sand-linen-trousers', { status: 404 });
});
