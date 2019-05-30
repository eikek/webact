package webact.app

import minitest._

object MetaParserSpec extends SimpleTestSuite {

  test("script header parse with crlf") {
    val script = "#!/usr/bin/env bash\r\n#\r\n# <webact>\r\n# Enabled: True\r\n# Category: disk\r\n#\r\n# test\r\n# </webact>\r\n\r\necho 'Hello World!!!'"
    assertEquals(
      MetaParser.parseMeta(script),
      MetaHeader(Key.Enabled -> "True", Key.Category -> "disk", Key.Description -> "\n\ntest\n\n")
    )
  }


  test("script header parse with lf") {
    val script = "#!/usr/bin/env bash\n#\n# <webact>\n# Enabled: True\n# Category: disk\n#\r\n# test\n# </webact>\n\necho 'Hello World!!!'"
    assertEquals(
      MetaParser.parseMeta(script),
      MetaHeader(Key.Enabled -> "True", Key.Category -> "disk", Key.Description -> "\n\ntest\n\n")
    )
  }


}
