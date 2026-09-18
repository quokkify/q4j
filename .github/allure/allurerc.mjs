export default {
  name: "Q4J",
  output: "./allure-report",
  plugins: {
    awesome: {
      options: {
        reportName: "Q4J test report",
        singleFile: false,
        reportLanguage: "en",
        groupBy: [
          "environment",
          "suite",
          "subSuite",
          "epic",
          "feature",
          "story",
        ],
      },
    },
  },
};
