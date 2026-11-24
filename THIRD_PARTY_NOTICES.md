# 第三方组件与许可

本应用包含以下第三方组件，均按其原始许可证使用。完整许可证文本随组件附于下方。

## ONNX Runtime

- 用途：在设备端执行验证码识别模型（`com.microsoft.onnxruntime:onnxruntime-android`）。
- 许可证：MIT
- 项目主页：https://github.com/microsoft/onnxruntime

## ddddocr

- 用途：登录验证码识别模型 `app/src/main/assets/ddddocr_captcha.onnx`，及其预处理与解码方式（见 `app/src/main/java/com/ty/gkschedule/captcha/CaptchaOcr.kt`）。
- 说明：原模型 `common_old.onnx` 已裁剪输出分类头（仅保留 blank 与 `A-Za-z0-9` 共 63 类）以减小体积，未改变其行为许可。
- 许可证：MIT
- 项目主页：https://github.com/sml2h3/ddddocr

---

## MIT License

Copyright (c) Microsoft Corporation（ONNX Runtime）
Copyright (c) sml2h3（ddddocr）

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
