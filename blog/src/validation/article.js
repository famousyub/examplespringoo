const { isMongoId, isLength } = require('validator')

const isNotEmpty = require('../validation/is-not-empty-string')

exports.getByUsers = (users) => (
  isNotEmpty(users) && users.split(',').length > 0
)

exports.create = (title, body, image, category) => (
  isNotEmpty(title) && isLength(title, { min: 3, max: 100 }) &&
  isNotEmpty(body) && isLength(body, { min: 10 }) &&
  isNotEmpty(image) &&
  (category === undefined || isNotEmpty(category) && isMongoId(category))
)

exports.update = (id, title, body, category) => (
  isMongoId(id) &&
  isNotEmpty(title) && isLength(title, { min: 3, max: 100 }) &&
  isNotEmpty(body) && isLength(body, { min: 10 }) &&
  (category === undefined || isNotEmpty(category) && isMongoId(category))
)

exports.image = (image) => image === undefined || isNotEmpty(image)
