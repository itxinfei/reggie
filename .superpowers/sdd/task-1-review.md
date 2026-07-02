=== Commits ===
84d2fc6 chore: add security dependencies (validation, jasypt)

=== Diff Stat ===
 pom.xml | 13 +++++++++++++
 1 file changed, 13 insertions(+)

=== Diff (context=10) ===
diff --git a/pom.xml b/pom.xml
index a891176..c8e444a 100644
--- a/pom.xml
+++ b/pom.xml
@@ -38,20 +38,33 @@
             <groupId>org.springframework.boot</groupId>
             <artifactId>spring-boot-starter</artifactId>
         </dependency>
 
         <dependency>
             <groupId>org.springframework.boot</groupId>
             <artifactId>spring-boot-starter-test</artifactId>
             <scope>test</scope>
         </dependency>
 
+        <!-- 参数校验 -->
+        <dependency>
+            <groupId>org.springframework.boot</groupId>
+            <artifactId>spring-boot-starter-validation</artifactId>
+        </dependency>
+
+        <!-- 配置加密 -->
+        <dependency>
+            <groupId>com.github.ulisesbocchio</groupId>
+            <artifactId>jasypt-spring-boot-starter</artifactId>
+            <version>3.0.3</version>
+        </dependency>
+
         <dependency>
             <groupId>com.h2database</groupId>
             <artifactId>h2</artifactId>
             <scope>test</scope>
         </dependency>
 
         <dependency>
             <groupId>org.springframework.boot</groupId>
             <artifactId>spring-boot-starter-web</artifactId>
             <scope>compile</scope>
