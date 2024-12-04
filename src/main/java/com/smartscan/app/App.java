package com.smartscan.app;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
	private static final String LOCAL_REPO_PATH = "C:/Users/Aravind/eclipse-workspace/Smart-scan/";
	private static final String LOCAL_TESTS_PATH = "C:/Users/Aravind/eclipse-workspace/Smart-scan/src/test/java";

	public static void main(String[] args) {
		try {
			// Get modified and newly added files
			Set<String> changedFiles = getChangedFiles();

			if (changedFiles.isEmpty()) {
				System.out.println("No changes detected.");
				return;
			}

			System.out.println("Changed files: " + changedFiles);

			// Analyze affected methods
			Set<String> affectedMethods = new HashSet<>();
			for (String filePath : changedFiles) {
				Path absolutePath = Paths.get(LOCAL_REPO_PATH, filePath);
				analyzeFileForMethods(absolutePath, affectedMethods);
			}

			System.out.println("Affected methods: " + affectedMethods);

			// Find and run related test cases
			Set<String> relatedTestClasses = findRelatedTestCases(affectedMethods);
			System.out.println("Related test classes: " + relatedTestClasses);

			executeTestCases(relatedTestClasses);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Fetches modified and newly added Java files in the repository.
	 */
	private static Set<String> getChangedFiles() throws Exception {
		File repoDir = new File(LOCAL_REPO_PATH);
		Git git = Git.open(repoDir);

		Status status = git.status().call();
		Set<String> changedFiles = new HashSet<>(status.getModified());
		changedFiles.addAll(status.getUntracked());
		changedFiles.addAll(status.getAdded());

		// Filter only Java files
		return changedFiles.stream().filter(file -> file.endsWith(".java")).collect(Collectors.toSet());
	}

	/**
	 * Analyzes a file to collect method declarations.
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
	 * Finds related test cases by matching affected methods to test methods.
	 */
	private static Set<String> findRelatedTestCases(Set<String> affectedMethods) {
		Set<String> relatedTests = new HashSet<>();
		try {
			Files.walk(Paths.get(LOCAL_TESTS_PATH)).filter(path -> path.toString().endsWith(".java"))
					.forEach(testFile -> {
						try {
							String content = Files.readString(testFile);
							CompilationUnit cu = StaticJavaParser.parse(content);

							// Get the class name and ensure it's a valid test class (ends with 'Test')
							String className = cu.getPrimaryTypeName().orElse("");
							if (className.startsWith("Test") || className.endsWith("Test")) {
								// Search for @Test annotation and check method invocations
								cu.accept(new VoidVisitorAdapter<Void>() {
									@Override
									public void visit(MethodDeclaration md, Void arg) {
										super.visit(md, arg);

										// Check if the method has @Test annotation
										if (md.isAnnotationPresent(org.junit.Test.class)) {
											// Check if any affected method is invoked inside this method
											md.getBody().ifPresent(body -> body.accept(new VoidVisitorAdapter<Void>() {
												@Override
												public void visit(com.github.javaparser.ast.expr.MethodCallExpr mce,
														Void arg) {
													super.visit(mce, arg);
													// Check if the method call matches any affected method
													if (affectedMethods.contains(mce.getNameAsString())) {
														relatedTests.add(className);
													}
												}
											}, arg));
										}
									}
								}, null);
							}
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
	 * Executes the related test cases using Maven.
	 */
	private static void executeTestCases(Set<String> testClasses) {
		try {
			for (String testClass : testClasses) {
				System.out.println("Running test class: " + testClass);
				ProcessBuilder builder = new ProcessBuilder("mvn", "test", "-Dtest=" + testClass);
				builder.directory(new File(LOCAL_REPO_PATH));
				Process process = builder.start();
				process.waitFor();
				System.out.println("Executed test class: " + testClass);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
