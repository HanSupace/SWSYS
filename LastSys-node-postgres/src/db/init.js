require('dotenv').config();

const fs = require('fs/promises');
const path = require('path');
const pool = require('./pool');

async function main() {
  const schema = await fs.readFile(path.join(__dirname, 'schema.sql'), 'utf8');
  await pool.query(schema);
  await pool.end();
  console.log('PostgreSQL schema is ready.');
}

main().catch(async (error) => {
  console.error(error);
  await pool.end();
  process.exit(1);
});
