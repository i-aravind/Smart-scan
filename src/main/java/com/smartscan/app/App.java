package com.smartscan.app;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class App {

	private static final String REPO_PATH = "path/to/your/repo"; // Update with your repository path
	private static final String TESTS_PATH = "path/to/your/tests"; // Update with your test files path

	public static void main(String[] args) {
		try {
			// Open Git repository
			File repoDir = new File(REPO_PATH);
			Git git = Git.open(repoDir);

			// Fetch changed files
			Status status = git.status().call();
			Set<String> modifiedFiles = status.getModified();
			modifiedFiles.addAll(status.getAdded());

			if (modifiedFiles.isEmpty()) {
				System.out.println("No changes detected.");
				return;
			}

			System.out.println("Changed files: " + modifiedFiles);

			// Analyze each changed file for affected methods
			Set<String> affectedMethods = new HashSet<>();
			for (String filePath : modifiedFiles) {
				if (filePath.endsWith(".java")) {
					analyzeFileForMethods(Paths.get(REPO_PATH, filePath), affectedMethods);
				}
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

	private static void analyzeFileForMethods(Path filePath, Set<String> affectedMethods) {
		try {
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

	private static Set<String> findRelatedTestCases(Set<String> affectedMethods) {
		Set<String> relatedTests = new HashSet<>();
		try {
			Files.walk(Paths.get(TESTS_PATH)).filter(path -> path.toString().endsWith(".java")).forEach(testFile -> {
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

	private static void runTestCase(String testClassName) {
		try {
			System.out.println("Running test case: " + testClassName);
			Class<?> testClass = Class.forName(testClassName);
			Object testInstance = testClass.getDeclaredConstructor().newInstance();

			Arrays.stream(testClass.getDeclaredMethods())
					.filter(method -> method.isAnnotationPresent(org.junit.Test.class)).forEach(method -> {
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
