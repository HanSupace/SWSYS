# Static asset structure

- `css/base`: application-wide layout and design foundations
- `css/pages`: styles owned by a page or page family
- `css/shared`: reusable component styles
- `js/pages`: page entry points and page-only behavior
- `js/shared`: reusable browser-side data and utilities
- `images`: image assets referenced by templates and styles

Templates should contain markup and server-rendered data only. Executable JavaScript and CSS belong in the directories above. The emotion catalog's `application/json` script is intentionally kept in the fragment as serialized server data, not executable code.
