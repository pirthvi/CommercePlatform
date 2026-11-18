# Commerce Platform

Commerce Platform runs one configurable store per backend and database. The customer storefront and the merchant Studio are separate Angular applications backed by a Spring Boot REST API. Everyday Apparel is the sample shop. Change the shop name and logo in **Studio → Site content → Logo**.

## Run locally

Use Java 17 or later, Maven, and Node.js 22.12 or later. Start Spring Boot from the `backend` directory so its relative database path resolves to the existing store.

| Application | Address | Start command |
| --- | --- | --- |
| Spring Boot API | http://localhost:8080/api | `mvn spring-boot:run` from `backend`, or open `backend` in IntelliJ and run **Commerce Platform API** |
| Customer storefront | http://localhost:4200 | `npm run dev` from `frontend` starts both Angular apps |
| Merchant Studio | http://localhost:4201 | The same `npm run dev` process serves Studio |

Run `npm install` in `frontend` on first setup. The development servers proxy `/api` and `/sitemap.xml` to port 8080. Studio sign-in uses the admin credentials configured in the backend environment through `ADMIN_USERNAME` and `ADMIN_PASSWORD`.

New local stores use `backend/data/commerce.mv.db`. An installation with only the previous database file continues to use it. An explicit `SPRING_DATASOURCE_URL` takes precedence. Set `JWT_SECRET` for deployed instances. Local development creates a persistent signing key under `backend/data` when no secret is configured.

## Manage the store

| Studio location | What you can change |
| --- | --- |
| Products → Categories | Create and rename categories. Renaming a category preserves product assignments and its storefront URL. Reassign its products and remove its references from menus and page components before deleting it. |
| Products → All products | Assign a category and manage price, visibility, product details, main photo and gallery uploads. |
| Products → Inventory | Select a product, enter a positive or negative quantity and a reason, and review the resulting balance and recent movements. Sales, cancellations and reopened orders also appear here. |
| Pages → Home page sections | Open the Home layout. Add reusable components, upload photos, select products or categories, change their order and preview edits before publishing. |
| Pages → Product page | Arrange the gallery, purchase controls, related products and other components. Use **Edit product wording and icons** for labels, symbols and uploaded icons. |
| Pages → Shop all clothing | Open **Catalog** to choose which categories appear in the shop. The starter shows Tops and Trousers. The **Related products** component on the product page has the same category controls. An empty selection includes all active products. |
| Pages → All pages | Select a page or create one. Choose **Add component**, edit its fields and use **Preview changes**. Open **Page settings** to set the title, URL, search description and publication status. **Publish changes** saves the layout and uploads together. |
| Site content | Edit Logo, Navigation, header controls and Footer. Category links use stable category slugs. |
| Settings → Interface text | Edit storefront and Studio wording by component, including form labels and error messages. Keep placeholders such as `{count}` and `{amount}` when editing text. |
| Settings → Commerce settings | Set the catalog currency, delivery fee, optional free-delivery threshold, storefront theme color and font style. Currency selection changes the denomination of catalog prices. Existing receipts retain their recorded currency and amounts. |
| Settings → Sitemap | Set the public storefront domain, inspect the scheduled job and its last/next run, and generate the sitemap immediately. The job rebuilds from published pages and active products on startup and daily at 03:00 UTC by default. |
| Orders | Review orders and update fulfilment status. Cancelling an order returns its units to stock. Reopening checks availability again. |

On its first startup with the apparel starter, Spring Boot installs four purchasable styles across Tops and Trousers, including prices, opening stock, fabric, fit, care instructions and two photos per style. The Home collection points to those product IDs. The shop, product, cart, checkout, sign-in, registration, account, confirmation, about, delivery and order help pages come with editable components and photography. Studio and the storefront serve the photos from the database.

The starter also upgrades existing stores. It adds missing sample styles alongside the current catalog, converts the built-in page copy, and supplies missing page imagery. A database version records the completed setup, so a restart preserves later Studio edits, removals and stock changes. Edit the collection in **Products**, the layouts in **Pages**, and shared branding and menus in **Site content**.

## Data and API

The API verifies prices and stock at checkout, records the order and inventory movements, and clears the customer's or guest's database cart in the same transaction. Checkout supports cash on delivery. Customers and Studio administrators sign in separately and use role-scoped JWT bearer tokens. A guest cart uses an opaque `X-Guest-Cart-Id`; customer sign-in merges it into the customer's cart.

| Resource | Public reads or actions | Studio updates |
| --- | --- | --- |
| Catalog | `GET /api/categories`, `GET /api/products?categoryId={id}`, `GET /api/products/slug/{slug}` | `/api/admin/categories`, `/api/admin/products` |
| Stock | Product availability is included in the catalog | `POST /api/admin/inventory/{id}/adjustments`, `GET /api/admin/inventory/{id}/movements` |
| Content | `/api/content/site`, `/api/content/{slug}`, `/api/content/product-page` | `/api/admin/pages`, `/api/admin/site-content/{section}`, `PUT /api/admin/product-page` |
| Page layouts | `GET /api/content/layouts/page/{slug}`, `GET /api/content/layouts/template/{key}` | `GET`, `POST` and `PUT` under `/api/admin/page-layouts` |
| Settings and wording | `GET /api/content/interface/{surface}` | `PATCH /api/admin/interface/{surface}`, `PUT /api/admin/commerce` |
| Sitemap | `GET /sitemap.xml` or `GET /api/sitemap.xml` | `GET`, `PUT` and `POST` under `/api/admin/sitemap` |
| Accounts, carts and orders | `/api/auth`, `/api/cart`, `/api/orders`, `/api/account/orders` | `POST /api/admin/session`, `/api/admin/orders` |

Angular components have adjacent `.component.ts`, `.component.html` and `.component.css` files. Home sections live in `frontend/apps/storefront/src/app/home` and the shared header, footer and brand components in `layout`. Global stylesheets contain shared design rules. The `frontend/shared` directory contains the configuration, text and money formatting used by the two apps. Backend resources in `commerce/` seed missing database entries on startup. Studio edits are stored in the database.

Run `mvn package` from `backend` and `npm run build:storefront` and `npm run build:admin` from `frontend` for production builds. Configure deployment origins, database connection, admin credentials and a private `JWT_SECRET` through the backend environment. `SITEMAP_CRON` can override the six-field Spring cron expression; `SITEMAP_SITE_URL` can provide the initial public storefront origin. The deployed storefront host must route `/sitemap.xml` to the backend, as the development proxy does.

After packaging the API, run `node --test tests/commerce.test.cjs` from `backend`. The tests launch the packaged application on a temporary loopback port with its own disposable database. They exercise the apparel starter, image delivery, page publishing, stock and checkout, then restart the backend to verify that merchant edits persist.
