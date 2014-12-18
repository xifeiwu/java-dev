/*
 * @method MD5
 *  计算某个字符串的MD5值
 * @param str
 *  待计算的字符串
 * @param encoding
 *  编码方式，默认为hex，该参数可省略
 * @return md5
 *  返回md5校验值
 */
function MD5(str, encoding) {
  // return crypto.createHash('md5').update(str).digest(encoding || 'hex');
  var buf = new mBuffer(1024);  
  var len = buf.write(str,0);  
  str = buf.toString('binary', 0, len);  
  var md5sum = crypto.createHash('md5');
  md5sum.update(str);
  str = md5sum.digest('hex');
  return str;
}
