import { expect, test } from '@playwright/test'
import { createServer, type Server } from 'node:http'

const apiKey = process.env.RELAY_BOOTSTRAP_API_KEY ?? 'rk_local_dev_key'
let receiver: Server
let receivedDeliveries = 0
let callbackPath = ''

test.beforeAll(async () => {
  receiver = createServer((request, response) => {
    request.resume()
    request.on('end', () => {
      if (request.url === callbackPath) {
        receivedDeliveries += 1
      }
      response.writeHead(204)
      response.end()
    })
  })

  await new Promise<void>((resolve, reject) => {
    receiver.once('error', reject)
    receiver.listen(9876, '0.0.0.0', resolve)
  })
})

test.afterAll(async () => {
  await new Promise<void>((resolve, reject) => {
    receiver.close((error) => (error ? reject(error) : resolve()))
  })
})

test('creates an endpoint, captures and delivers a webhook, then replays it', async ({
  page,
  request,
}, testInfo) => {
  const endpointName = `Orders API ${Date.now()}`
  callbackPath = `/webhooks/${Date.now()}-${testInfo.retry}`
  const receiverUrl = `http://host.docker.internal:9876${callbackPath}`

  await page.goto('/sign-in')
  await page.getByLabel('API key').fill(apiKey)
  await page.getByRole('button', { name: 'Continue' }).click()
  await expect(page).toHaveURL('/')

  await page.goto('/endpoints')
  await page.getByPlaceholder('New endpoint name, e.g. Stripe webhooks').fill(endpointName)
  await page.getByRole('button', { name: 'Create' }).click()

  const endpointLink = page.getByRole('link', { name: new RegExp(endpointName) })
  await expect(endpointLink).toBeVisible()
  await endpointLink.click()
  await expect(page.getByRole('heading', { name: endpointName })).toBeVisible()

  await page.getByRole('button', { name: 'Add destination' }).click()
  await page.getByPlaceholder('Orders service').fill('Order receiver')
  await page.getByPlaceholder('https://…').fill(receiverUrl)
  await page.getByRole('button', { name: 'Save destination' }).click()
  await expect(page.getByText('Order receiver')).toBeVisible()

  const ingestCard = page.locator('.card').filter({ hasText: 'Ingest URL' })
  const ingestUrl = await ingestCard.locator('code').textContent()
  expect(ingestUrl).toBeTruthy()

  const ingestResponse = await request.post(ingestUrl!, {
    data: { type: 'order.created', orderId: 'order-1042' },
  })
  expect(ingestResponse.status()).toBe(202)

  await page.reload()
  const eventLink = page.locator('.feed a').first()
  await expect(eventLink).toContainText('POST')
  const eventHref = await eventLink.getAttribute('href')
  expect(eventHref).toMatch(/^\/events\/[0-9a-f-]+$/)
  const eventId = eventHref!.split('/').at(-1)!

  await expect
    .poll(
      async () => {
        const response = await request.get(`/api/events/${eventId}`, {
          headers: { 'X-Api-Key': apiKey },
        })
        const detail = await response.json()
        return detail.deliveries[0]?.status
      },
      { timeout: 15_000 },
    )
    .toBe('DELIVERED')
  await expect.poll(() => receivedDeliveries).toBe(1)

  await eventLink.click()
  await expect(page.getByRole('heading', { name: 'Deliveries' })).toBeVisible()
  await expect(page.getByText('Order receiver')).toBeVisible()
  await expect(page.getByText('delivered', { exact: true })).toBeVisible()
  await expect(page.getByText('Attempt 1')).toBeVisible()

  await page.getByRole('button', { name: 'Replay' }).click()
  await expect(page.getByText('Re-queued 1 delivery(ies).')).toBeVisible()
  await expect.poll(() => receivedDeliveries, { timeout: 15_000 }).toBe(2)
})