---
layout: post
title: Integration Tests made easy (and automated)
---

In the dark ages of software development those who wanted to test their pile of code against a database or remote systems had to put a lot of effort in that.  
Building a VM from an image, installing all the stuff they needed and give to admin so it could be hosted on server, also hoping that no colleague would leave the VM in an unusable state.    
Nowadays we have it a lot easier than that. Thanks to our modern tools we can easily put together a VM with all the things we need for our own, test against it and than start all over in a proper time.
Nonetheless I still see sometimes colleagues fighting about the ownership of a VM or hesitate when it comes to establish integration testing.  
This is why I wrote this article. You'll find the complete source code at [GitHub](https://github.com/marcuslang/persistence-perf-test).

#### goal
What I want you to show is how to automate your integration testing with tools taking care of starting, preparing and cleaning VMs for you. At the end we will have a test that runs against a freshly installed PostGreSQL DB on a VM with all the data we needed in the DB.  
In order to achive that goal we will let
 * Vagrant control the VM
 * Ansible do the provision of the stuff we need on the VM (in this case a PostGreSQL DB)
 * Maven Exec start the VM before and stop the VM after the tests
 * Maven Failsafe run the tests

#### the vagrant part
[Vagrant](https://www.vagrantup.com/) needs to be installed on your system. It will also need a provider like [VirtualBox](https://www.virtualbox.org/) or [VMWare](http://www.vmware.com/).
[Ansible](https://www.ansible.com/) will do the provision of the system. Since it lacks native support for Windows, we will let Vagrant install Ansible as a local provisioner on the VM. 
This will start us a VM that will be provisioned via Ansible.  
```
Vagrant.configure("2") do |config|

  config.vm.define "postgresql" do |postgresql|
    postgresql.vm.box = "ubuntu/trusty64"
    postgresql.vm.network "private_network", ip: "10.1.1.200"
    postgresql.vm.provider :virtualbox do |vb|
      vb.customize ["modifyvm", :id, "--cpus", "2", "--memory", "2048"]
    end
  end

  config.vm.provision "ansible_local" do |prov|
    prov.install = true
    prov.playbook = "/vagrant/ansible/playbook.yml"
    config.ssh.shell = "bash -c 'BASH_ENV=/etc/profile exec bash'"
  end
end
```

#### the ansible part
Ansible depends on a so called [Playbook](http://docs.ansible.com/ansible/playbooks.html). The Playbook will tell Ansible which hosts should be provisioned and what it has to do on this hosts. 
In this example the Playbook contains one role for installing PostGreSQL by [ANXS](https://github.com/ANXS/postgresql.git) and another role that takes care of creating the database schema with user and testdata, that is based on the USDA example 
database dump from [PgFoundry](http://pgfoundry.org/frs/shownotes.php?release_id=389).
I don't want to go into much more detail about how these roles do their thing at this time, but if you want to take a closer look go to my repository at GitHub.

Basically the YAML file tells Ansible to become superuser on host *postgresql* and then do an update. After that it will start with the roles. The configuration of user, password or database name is configured outside of the roles in **vars_files**.
```yaml
- hosts: postgresql
  become: True
  vars_files:
      - ./vars_postgresql.yml
      - ./vars_database_init.yml
  pre_tasks:
    - name: Update apt
      become: yes
      apt:
        cache_valid_time: 1800
        update_cache: yes
      tags:
        - build
  roles:
  - role: postgresql
    download_directory: "~/"
  - role: database_init
```

#### the exec plugin part
[Exec-Maven-Plugin:](http://www.mojohaus.org/exec-maven-plugin/) This Maven plugin is great for executing external programs. In our example it starts and stops (more destroys) the Vagrant VM in the pre- and post-integration-test phase of Maven.
First execution will create and provision the VM, while the second will destroy the VM. Therefore we will have a fresh VM every time we start our tests. 
```XML
<plugin> <!-- start / stop our vm -->
        <artifactId>exec-maven-plugin</artifactId>
        <groupId>org.codehaus.mojo</groupId>
        <executions>
          <execution>
            <id>start of database vm</id>
            <phase>pre-integration-test</phase>
            <goals>
              <goal>exec</goal>
            </goals>
            <configuration>
              <workingDirectory>${main.basedir}/integration/database</workingDirectory>
              <executable>vagrant</executable>
              <arguments>
                <argument>up</argument>
                <argument>postgresql</argument>
                <argument>--provision</argument> 
              </arguments>
            </configuration>
          </execution>
          <execution>
            <id>shutdown and destroy database vm</id>
            <phase>post-integration-test</phase>
            <goals>
              <goal>exec</goal>
            </goals>
            <configuration>
              <workingDirectory>${main.basedir}/integration/database</workingDirectory>
              <executable>vagrant</executable>
              <arguments>
                <argument>destroy</argument>
                <argument>-f</argument>
                <argument>postgresql</argument>
              </arguments>
            </configuration>
          </execution>
        </executions>
      </plugin>
```

#### the failsafe plugin part
Now to the Maven plugin that takes care of executing all integration tests. The configuration below looks for test classes in packages containing *integration* in the path. Those tests will be run in the integration-test phase of Maven. 
Additionally I've set the jdbc connection information as system properties for an easier access inside the tests. 
In case you want to know why failsafe and not surefire: Surefire would stop if any test fails, that great for unit tests because we want fast feedback. But here we want to make sure that our VM is destroyed at the end, leaving a clean space.
Failsafe will run every test defined, if some of them fail it will still go through till the end.
```XML
 <plugin> <!-- run the integration test -->
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-failsafe-plugin</artifactId>
        <version>2.19.1</version>
        <configuration>
          <excludes>
            <exclude>**/integration/**.java</exclude>
          </excludes>
        </configuration>
        <executions>
          <execution>
            <id>integration-test-postgresql</id>
            <goals>
              <goal>integration-test</goal>
            </goals>
            <phase>integration-test</phase>
            <configuration>
              <excludes>
                <exclude>none</exclude>
              </excludes>
              <includes>
                <include>**/integration/**</include>
              </includes>
              <systemPropertyVariables>
                <database.type>postgresql</database.type>
                <database.url>jdbc:postgresql://10.1.1.200:5432/testdb</database.url>
                <database.user>dbuser</database.user>
                <database.password>dbuser</database.password>
                <database.testquery>SELECT 1</database.testquery>
              </systemPropertyVariables>
            </configuration>
          </execution>
        </executions>
      </plugin>
```

### recap
I'll hope this article got you an overview how to test your code against a fully automated test environment. 
In my opinion the great thing is that every time you start the integration tests with Maven, a complete new VM is build, containing 
a fresh setup of the things you need. That gives you a good starting point for robust, repeatable tests that rely on 
specific, provided components, e.g. a database (yea I know there is HSQLDB but sometimes you want a real database, sorry).
These test obviously will take more time than your usual unit test, but it's still a convenient way for nightly builds in 
your CI or if you wanted to grab a coffee anyway.
