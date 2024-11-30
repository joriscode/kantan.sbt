credentials += Credentials(
  "GitHub Package Registry",
  "maven.pkg.github.com",
  "_", // user is ignored
  sys.env("REPO_GITHUB_TOKEN")
)
