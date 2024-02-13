lib: rec {
  version = "0.6.0-SNAPSHOT";
  latest-release = "0.5.2";
  license = lib.licenses.gpl3;
  homepage = https://github.com/eikek/webact;

  meta-src = {
    inherit license homepage;
    description = ''
      Run actions from the web. Webact allows to manage scripts on the
      server and execute them. This package is build from sources.
    '';
  };

  meta-bin = {
    inherit license homepage;
    description = ''
      Run actions from the web. Webact allows to manage scripts on the
      server and execute them. This package is build from published
      zip files.
    '';
  };
}
