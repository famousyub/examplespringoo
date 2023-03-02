module.exports = async (ctx, next) => {
  try {
    await next()
  } catch(e) {
    console.log(e)
    ctx.body = { error: e.message || 'Internal Server Error' }
    ctx.status = e.status || 500
  }
}
