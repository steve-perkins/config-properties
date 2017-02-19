config-properties
=================
This is one of three Git repositories, which work together to demonstrate using [Consul](https://www.consul.io) and 
[Vault](https://www.vaultproject.io) for configuration management:

* https://github.com/steve-perkins/config-properties - Contains:
  * Properties files for every environment (e.g. "dev", "staging", "production") in a hypothetical enterprise, and 
    every application within each environment.
  * A processor script, run by Gradle, which syncs all of the property information with Consul every time the Git 
    repository is updated.
* https://github.com/steve-perkins/config-client-lib - A client library which loads the appropriate config properties 
  for a given environment and application from Consul and Vault.
* https://github.com/steve-perkins/config-sample-app - A sample web application which retrieves its config properties 
  using the client library, and displays them in the browser.
  
This demo shows the use case of having two types of config properties:

1. **non-secret** values, which can and should be maintainable by developer teams (e.g. JDBC URL's).
2. **secret** values, which should only be viewable or maintainable by people with specialized access (e.g. 
   usernames and passwords)
   
The non-secret values are stored as-is in the `config-properties` repo, and loaded directly into Consul.  For *secret* 
values, Git (and Consul) store a Vault path for that property.  When the `config-client-lib` library encounters a 
secret, it loads the "true" value from this Vault path... and applications such as `config-sample-app` are none the 
wiser.

Setup
=====
1. Download [Consul](https://www.consul.io/downloads.html) and [Vault](https://www.vaultproject.io/downloads.html).  They 
   each require no installation, just copy them to some location(s) locally.
2. In a command-line shell, startup Consul like this:

```
consul agent -dev
```

2. In another shell, startup Vault like this:  

```
vault server -dev
```

3. In yet another shell, execute the following setup commands:

```
export VAULT_ADDR=http://127.0.0.1:8200 (Linux / OS X)
or
set VAULT_ADDR=http://127.0.0.1:8200 (Windows)
```
... and then...
```
vault auth-enable userpass
vault policy-write writers writers.hcl
vault write auth/userpass/users/vault_user password=vault_pass policies=writers

vault write secret/application jdbcUsername=scott jdbcPassword=tiger
```
The `writers.hcl` file is located in the root of this repo.  This policy grants our test user access to a path branch 
in Vault's tree-like structure.  In real life, you might want to create separate policies for the various environments 
and/or applications (at *some* level of granularity), so that you can control access and prevent one application from 
being able to read another one's secrets.

4. The repo is a Gradle-based project, and its build script includes a `runProcessor` task that executes the config 
   processor.

```
gradlew runProcessor
```

In real life, you might want to setup a job for this repo in your continuous integration server of choice (e.g. 
Jenkins), to automatically re-run the processor everytime a Git hook indicates that a new properties change has been 
committed to the repo.

5. See the [config-client-lib](https://github.com/steve-perkins/config-client-lib) 
   and [config-sample-app](https://github.com/steve-perkins/config-sample-app) project README's for next steps.


