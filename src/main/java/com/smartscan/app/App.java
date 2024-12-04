package com.smartscan.app;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class App {
    private static final String LOCAL_REPO_PATH = "C:/Users/Aravind/eclipse-workspace/Smart-scan/"; // Update to your local Git repo path
    private static final String LOCAL_TESTS_PATH = "C:/Users/Aravind/eclipse-workspace/Smart-scan/src/test/java"; // Update to your local test files path

    public static void main(String[] args) {
        try {
            // Open the local Git repository
            File repoDir = new File(LOCAL_REPO_PATH);
            Git git = Git.open(repoDir);

            // Fetch the modified or added files
            Status status = git.status().call();
            Set<String> modifiedFiles = new HashSet<>(status.getModified());
            modifiedFiles.addAll(status.getAdded());

            if (modifiedFiles.isEmpty()) {
                System.out.println("No changes detected.");
                return;
            }else {
            	modifiedFiles = modifiedFiles.stream().filter(f -> f.endsWith(".java")).collect(Collectors.toSet());
            }

            System.out.println("Changed files: " + modifiedFiles);

            // Analyze each changed file for affected methods
            Set<String> affectedMethods = new HashSet<>();
            for (String filePath : modifiedFiles) {
                Path absolutePath = Paths.get(LOCAL_REPO_PATH, filePath);
                analyzeFileForMethods(absolutePath, affectedMethods);
            }

            System.out.println("Affected methods: " + affectedMethods);

            // Find related test cases based on method invocations
            Set<String> relatedTestClasses = findRelatedTestCases(affectedMethods);

            System.out.println("Related test classes: " + relatedTestClasses);

            // Run related test cases
            for (String testClass : relatedTestClasses) {
                runTestCase(testClass);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Analyzes a file for method declarations and collects the method names.
     */
    private static void analyzeFileForMethods(Path filePath, Set<String> affectedMethods) {
        try {
            if (!Files.exists(filePath)) {
                System.err.println("File does not exist: " + filePath);
                return;
            }

            String content = Files.readString(filePath);
            CompilationUnit cu = StaticJavaParser.parse(content);

            cu.accept(new VoidVisitorAdapter<Set<String>>() {
                @Override
                public void visit(MethodDeclaration md, Set<String> collector) {
                    super.visit(md, collector);
                    collector.add(md.getNameAsString());
                }
            }, affectedMethods);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Finds related test classes based on affected methods and their invocations in test files.
     */
    private static Set<String> findRelatedTestCases(Set<String> affectedMethods) {
        Set<String> relatedTests = new HashSet<>();
        try {
            Files.walk(Paths.get(LOCAL_TESTS_PATH))
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(testFile -> {
                        try {
                            String content = Files.readString(testFile);
                            CompilationUnit cu = StaticJavaParser.parse(content);

                            cu.accept(new VoidVisitorAdapter<Set<String>>() {
                                @Override
                                public void visit(MethodDeclaration md, Set<String> collector) {
                                    super.visit(md, collector);

                                    // Check if the test method calls an affected method
                                    for (String method : affectedMethods) {
                                        if (md.toString().contains(method)) {
                                            collector.add(cu.getPrimaryTypeName().orElse("") + "Test");
                                        }
                                    }
                                }
                            }, relatedTests);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return relatedTests;
    }

    /**
     * Runs the test case using reflection.
     */
    private static void runTestCase(String testClassName) {
        try {
            System.out.println("Running test case: " + testClassName);
            Class<?> testClass = Class.forName(testClassName);
            Object testInstance = testClass.getDeclaredConstructor().newInstance();

            Arrays.stream(testClass.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(org.junit.Test.class))
                    .forEach(method -> {
                        try {
                            System.out.println("Executing test method: " + method.getName());
                            method.invoke(testInstance);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
