export default {
  name: "Q4J",
  output: "./allure-report",
  historyPath: "./allure-history/history.jsonl",
  historyLimit: 20,
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
